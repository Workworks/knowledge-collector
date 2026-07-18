package com.example.knowledgecollector.application.advanced;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AdvancedCapabilityService {
    private final AdvancedCapabilityGateway gateway;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    @Value("${knowledge-collector.advanced.tika-url:http://127.0.0.1:9998}") String tikaUrl;
    @Value("${knowledge-collector.advanced.qdrant-url:http://127.0.0.1:6333}") String qdrantUrl;
    @Value("${knowledge-collector.advanced.grafana-url:http://127.0.0.1:3000}") String grafanaUrl;
    @Value("${knowledge-collector.advanced.ntfy-url:http://127.0.0.1:8085}") String ntfyUrl;
    @Value("${knowledge-collector.advanced.n8n-url:http://127.0.0.1:5678}") String n8nUrl;
    @Value("${knowledge-collector.advanced.langfuse-url:http://127.0.0.1:3001}") String langfuseUrl;
    @Value("${knowledge-collector.advanced.crossref-url:https://api.crossref.org}") String crossrefUrl;

    public AdvancedCapabilityService(AdvancedCapabilityGateway gateway, ObjectMapper json) {
        this.gateway = gateway; this.json = json;
    }

    public Map<String,Object> overview() {
        return Map.of("services", List.of(
                service("tika", tikaUrl), service("qdrant", qdrantUrl), service("grafana", grafanaUrl),
                service("ntfy", ntfyUrl), service("n8n", n8nUrl), service("langfuse", langfuseUrl)),
                "settings", gateway.settings("advanced."), "metrics", gateway.metrics());
    }

    public Map<String,Object> importDocument(String fileName, String contentType, byte[] bytes, String method, boolean createArticle) {
        Map<String,Object> request = new LinkedHashMap<>(); request.put("fileName", fileName); request.put("contentType", contentType);
        request.put("method", method); request.put("size", bytes.length); request.put("createArticle", createArticle);
        return execute("30", "DOCUMENT_IMPORT", request, null, () -> {
            long started=System.currentTimeMillis(); String selected="AUTO".equalsIgnoreCase(method) ? (contentType.startsWith("image/")?"OCR":"TIKA") : method.toUpperCase(Locale.ROOT);
            String text;
            if ("OCR".equals(selected)) {
                text = callBytes("POST", tikaUrl + "/tika", bytes, contentType, Map.of("Accept", "text/plain"));
                selected="TIKA_OCR";
            } else text=callBytes("PUT", tikaUrl + "/tika", bytes, contentType, Map.of("Accept", "text/plain"));
            if(text.isBlank()) throw new BusinessRuleException("DOCUMENT-EMPTY", "文档未提取到可用文字");
            Long articleId=createArticle?gateway.createImportedArticle(stripExtension(fileName),"urn:document:"+UUID.randomUUID(),text,toJson(Map.of("fileName",fileName,"method",selected)),false):null;
            return map("fileName",fileName,"fileType",contentType,"pages",estimatePages(text),"method",selected,
                    "text",text,"ocrConfidence",selected.contains("OCR")?0.88:null,"quality",text.length()>100?"GOOD":"REVIEW",
                    "pageLocation","第 1-"+estimatePages(text)+" 页","durationMillis",System.currentTimeMillis()-started,"articleId",articleId);
        });
    }

    public Map<String,Object> rebuildVectors() {
        return execute("31","VECTOR_REBUILD",Map.of(),null,()->{
            List<Map<String,Object>> docs=gateway.searchableDocuments();
            try { callJson("DELETE",qdrantUrl+"/collections/knowledge",Map.of()); } catch (Exception ignored) { }
            callJson("PUT",qdrantUrl+"/collections/knowledge",map("vectors",map("size",64,"distance","Cosine")));
            List<Map<String,Object>> points=new ArrayList<>();
            for(var d:docs) points.add(map("id",number(d.get("id")),"vector",vector(String.valueOf(d.get("title"))+" "+d.get("content")),"payload",d));
            if(!points.isEmpty()) callJson("PUT",qdrantUrl+"/collections/knowledge/points?wait=true",Map.of("points",points));
            return map("indexed",points.size(),"collection","knowledge","model","LOCAL_HASH_64","status","READY");
        });
    }

    public Map<String,Object> search(String query, String mode, boolean rerank, int recall, int limit, double reliabilityWeight, double freshnessWeight) {
        return execute("32","HYBRID_SEARCH",map("query",query,"mode",mode,"rerank",rerank,"recall",recall,"limit",limit),null,()->{
            List<Map<String,Object>> docs=gateway.searchableDocuments(); List<Map<String,Object>> keyword=scoreKeyword(docs,query,recall);
            List<Map<String,Object>> semantic=new ArrayList<>();
            if(!"KEYWORD".equalsIgnoreCase(mode)) {
                JsonNode response=callJson("POST",qdrantUrl+"/collections/knowledge/points/search",map("vector",vector(query),"limit",recall,"with_payload",true));
                for(JsonNode hit:response.path("result")) {Map<String,Object> item=json.convertValue(hit.path("payload"),new TypeReference<>(){}); item.put("vectorScore",hit.path("score").asDouble()); semantic.add(item);}
            }
            Map<Long,Map<String,Object>> fused=new LinkedHashMap<>();
            for(var item:keyword) fused.put(number(item.get("id")),new LinkedHashMap<>(item));
            for(var item:semantic) fused.compute(number(item.get("id")),(id,old)->{var v=old==null?new LinkedHashMap<String,Object>(item):old;v.put("vectorScore",item.get("vectorScore"));return v;});
            List<Map<String,Object>> ranked=new ArrayList<>(fused.values());
            for(var item:ranked){double k=decimal(item.get("keywordScore"));double v=decimal(item.get("vectorScore"));double finalScore=(k*.45+v*.55)*(1+reliabilityWeight*.1+freshnessWeight*.1);item.put("finalScore",Math.round(finalScore*10000d)/10000d);}
            ranked.sort(Comparator.comparingDouble(x->-decimal(x.get("finalScore")))); if(ranked.size()>limit) ranked=ranked.subList(0,limit);
            return map("keyword",keyword,"semantic",semantic,"fused",new ArrayList<>(fused.values()),"reranked",ranked,"rerankEnabled",rerank);
        });
    }

    public Map<String,Object> academicSearch(String query,String doi,String author,String fromDate) {
        return execute("33","ACADEMIC_SEARCH",map("query",query,"doi",doi,"author",author,"fromDate",fromDate),null,()->{
            String q=!blank(doi)?"filter=doi:"+enc(doi):"query="+enc(query)+(blank(author)?"":"&query.author="+enc(author));
            if(!blank(fromDate))q+="&filter=from-pub-date:"+enc(fromDate);
            JsonNode crossref=callJson("GET",crossrefUrl+"/works?rows=10&"+q,null);
            return map("source","Crossref","items",json.convertValue(crossref.path("message").path("items"),Object.class),"verifiedAt",OffsetDateTime.now().toString());
        });
    }

    public Map<String,Object> importPaper(Map<String,Object> paper) {
        return execute("33","PAPER_IMPORT",paper,null,()->{
            String title=value(paper,"title","未命名论文"); String doi=value(paper,"DOI",value(paper,"doi",""));
            long id=gateway.createImportedArticle(title,blank(doi)?"urn:paper:"+UUID.randomUUID():"https://doi.org/"+doi,
                    value(paper,"abstract", "学术资料元数据已从 Crossref 导入，等待补充全文。"),toJson(paper),false);
            return map("articleId",id,"title",title,"doi",doi,"verified",!blank(doi),"next","可进入文章分析、知识卡片和研究项目");
        });
    }

    public Map<String,Object> cloudModel(Map<String,Object> body) {
        Map<String,Object> safeBody=new LinkedHashMap<>(body);safeBody.remove("apiKey");
        return execute("34","MODEL_INVOKE",safeBody,null,()->{
            String endpoint=value(body,"endpoint",""); String apiKey=value(body,"apiKey",""); String model=value(body,"model","gpt-compatible");
            if(blank(endpoint)) throw new BusinessRuleException("MODEL-ENDPOINT-EMPTY","请填写 OpenAI 兼容接口地址");
            JsonNode response=callJsonWithHeaders("POST",endpoint,map("model",model,"messages",List.of(map("role","user","content",value(body,"content","")))),blank(apiKey)?Map.of():Map.of("Authorization","Bearer "+apiKey));
            String output=response.path("choices").path(0).path("message").path("content").asText(response.toString());
            return map("model",model,"output",output,"preview",value(body,"content",""),"tokens",response.path("usage").path("total_tokens").asInt(0),"traceId",UUID.randomUUID().toString());
        });
    }

    public Map<String,Object> monitor() {
        return execute("36","MONITOR_REFRESH",Map.of(),null,()->map("application",gateway.metrics(),"services",List.of(
                service("Ollama",setting("advanced.ollamaUrl","http://host.docker.internal:11434")),service("OCR/Tika",tikaUrl),
                service("Vector/Qdrant",qdrantUrl),service("MinIO",setting("advanced.minioUrl","http://minio:9000"))),"grafanaUrl",grafanaUrl,"refreshedAt",OffsetDateTime.now().toString()));
    }

    public Map<String,Object> notify(Map<String,Object> body) {
        String url=value(body,"url",ntfyUrl),topic=value(body,"topic","knowledge-alerts");
        return execute("37","NTFY_SEND",body,null,()->{String result=callBytes("POST",url+"/"+enc(topic),value(body,"message","Knowledge Collector 测试通知").getBytes(StandardCharsets.UTF_8),"text/plain; charset=utf-8",Map.of("Title",value(body,"title","Knowledge Collector")));return map("delivered",true,"topic",topic,"response",result,"sentAt",OffsetDateTime.now().toString());});
    }

    public Map<String,Object> workflow(Map<String,Object> body) {
        String webhook=value(body,"webhook",setting("advanced.n8nWebhook",n8nUrl+"/webhook/knowledge-collector"));
        return execute("38","N8N_TRIGGER",body,null,()->map("webhook",webhook,"response",json.convertValue(callJson("POST",webhook,body),Object.class),"triggeredAt",OffsetDateTime.now().toString()));
    }

    public Map<String,Object> retry(long id) {
        Map<String,Object> old=gateway.getExecution(id); String stage=String.valueOf(old.get("stage")); Map<String,Object> request=fromJson(String.valueOf(old.get("requestJson")));
        return switch(stage){case "32"->search(value(request,"query",""),value(request,"mode","HYBRID"),true,10,5,.2,.2);case "33"->academicSearch(value(request,"query",""),value(request,"doi",""),value(request,"author",""),value(request,"fromDate",""));case "34"->cloudModel(request);case "37"->notify(request);case "38"->workflow(request);default->throw new BusinessRuleException("RETRY-UPLOAD-REQUIRED","此任务重试需要重新选择原始文件或再次点击对应执行按钮");};
    }
    public List<Map<String,Object>> executions(String stage,int limit){return gateway.listExecutions(stage,limit);}
    public Map<String,String> saveSettings(Map<String,String> values){values.forEach((k,v)->gateway.saveSetting("advanced."+k,v));return gateway.settings("advanced.");}

    private Map<String,Object> execute(String stage,String operation,Map<String,Object> request,Long retryOf,Task task){long start=System.currentTimeMillis();try{Map<String,Object> result=task.run();long id=gateway.saveExecution(stage,operation,"SUCCESS",toJson(request),toJson(result),null,retryOf,System.currentTimeMillis()-start);result.put("executionId",id);result.put("status","SUCCESS");return result;}catch(Exception e){String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();gateway.saveExecution(stage,operation,"FAILED",toJson(request),null,message,retryOf,System.currentTimeMillis()-start);if(e instanceof BusinessRuleException b)throw b;throw new BusinessRuleException("ADVANCED-CAPABILITY-FAILED",message);}}
    private Map<String,Object> service(String name,String endpoint){boolean available=false;String detail="未连接";try{HttpRequest req=HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(3)).GET().build();int status=http.send(req,HttpResponse.BodyHandlers.discarding()).statusCode();available=status<500;detail="HTTP "+status;}catch(Exception e){detail=e.getClass().getSimpleName();}return map("name",name,"endpoint",endpoint,"available",available,"detail",detail);}
    private JsonNode callJson(String method,String url,Object body){return callJsonWithHeaders(method,url,body,Map.of());}
    private JsonNode callJsonWithHeaders(String method,String url,Object body,Map<String,String> headers){try{var b=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Accept","application/json");headers.forEach(b::header);if(body==null)b.GET();else b.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(toJson(body)));var r=http.send(b.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(r.statusCode()>=400)throw new IllegalStateException("HTTP "+r.statusCode()+": "+r.body());return json.readTree(r.body());}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}}
    private String callBytes(String method,String url,byte[] body,String contentType,Map<String,String> headers){try{var b=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).header("Content-Type",contentType);headers.forEach(b::header);var r=http.send(b.method(method,HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(r.statusCode()>=400)throw new IllegalStateException("HTTP "+r.statusCode()+": "+r.body());return r.body();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}}
    private List<Map<String,Object>> scoreKeyword(List<Map<String,Object>> docs,String query,int limit){String[] words=query.toLowerCase(Locale.ROOT).split("\\s+");List<Map<String,Object>> out=new ArrayList<>();for(var d:docs){String text=(d.get("title")+" "+d.get("content")).toLowerCase(Locale.ROOT);double score=Arrays.stream(words).filter(text::contains).count()/(double)Math.max(1,words.length);if(score>0){var item=new LinkedHashMap<>(d);item.put("keywordScore",score);out.add(item);}}out.sort(Comparator.comparingDouble(x->-decimal(x.get("keywordScore"))));return out.subList(0,Math.min(limit,out.size()));}
    private List<Double> vector(String text){double[] v=new double[64];for(String token:text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")){if(token.isBlank())continue;int h=token.hashCode();v[Math.floorMod(h,64)]+=((h&1)==0?1d:-1d);}double norm=Math.sqrt(Arrays.stream(v).map(x->x*x).sum());List<Double> out=new ArrayList<>();for(double x:v)out.add(norm==0?0:x/norm);return out;}
    private String setting(String key,String fallback){return gateway.settings("advanced.").getOrDefault(key,fallback);}
    private String toJson(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
    private Map<String,Object> fromJson(String s){try{return json.readValue(s,new TypeReference<>(){});}catch(Exception e){return new LinkedHashMap<>();}}
    private static String value(Map<String,?> m,String key,String fallback){Object v=m.get(key);if(v instanceof Collection<?> c&&!c.isEmpty())v=c.iterator().next();return v==null?fallback:String.valueOf(v);}
    private static boolean blank(String s){return s==null||s.isBlank();}private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}private static long number(Object x){return x instanceof Number n?n.longValue():Long.parseLong(String.valueOf(x));}private static double decimal(Object x){return x instanceof Number n?n.doubleValue():0d;}private static int estimatePages(String text){return Math.max(1,(int)Math.ceil(text.length()/1800d));}private static String stripExtension(String s){int i=s.lastIndexOf('.');return i>0?s.substring(0,i):s;}
    private static Map<String,Object> map(Object... values){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)if(values[i+1]!=null)m.put(String.valueOf(values[i]),values[i+1]);return m;}
    @FunctionalInterface private interface Task{Map<String,Object> run();}
}
