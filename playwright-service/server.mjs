import http from "node:http";
import { chromium } from "playwright";

const reply=(res,status,value)=>{const body=JSON.stringify(value);res.writeHead(status,{"content-type":"application/json;charset=utf-8","content-length":Buffer.byteLength(body)});res.end(body);};
const server=http.createServer(async(req,res)=>{
  if(req.method==="GET"&&req.url==="/health")return reply(res,200,{status:"UP",browser:"chromium"});
  if(req.method!=="POST"||req.url!=="/render")return reply(res,404,{error:"not found"});
  let body="";for await(const chunk of req)body+=chunk;
  let browser;
  try{
    const input=JSON.parse(body),target=new URL(input.url);if(!["http:","https:"].includes(target.protocol))throw new Error("only HTTP/HTTPS URLs are allowed");
    browser=await chromium.launch({headless:true});const page=await browser.newPage({viewport:{width:1440,height:1000}});
    await page.goto(target.toString(),{waitUntil:"networkidle",timeout:Math.max(5,input.timeoutSeconds||45)*1000});
    const html=await page.content(),screenshot=await page.screenshot({fullPage:true,type:"png"});
    const metadata=await page.evaluate(()=>({title:document.title,author:document.querySelector('meta[name="author"]')?.content||null,publishedAt:document.querySelector('meta[property="article:published_time"]')?.content||null}));
    reply(res,200,{finalUrl:page.url(),html,screenshotBase64:screenshot.toString("base64"),...metadata});
  }catch(error){reply(res,422,{error:error.message});}finally{if(browser)await browser.close();}
});
server.listen(3003,"0.0.0.0");
