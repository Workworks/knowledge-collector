package com.example.knowledgecollector.capability.translation;

public interface TranslationProvider {
    TranslationResult translate(TranslationRequest request);

    record TranslationRequest(String text, String sourceLanguage, String targetLanguage) {
    }

    record TranslationResult(String translatedText, String detectedLanguage, String provider) {
    }
}
