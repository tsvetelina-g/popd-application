package app.popdapplication.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiRecommendationService {

    private final ChatModel chatModel;

    @Autowired
    public AiRecommendationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String getMovieRecommendations(String userQuery) {
        String promptTemplate = """
            You are a helpful movie recommendation assistant for a movie review platform called POPd.
            Based on the user's request: "{query}"
            
            Please provide 3-5 movie recommendations with brief explanations (1-2 sentences each).
            Format your response in a clear, friendly way. Be specific about why each movie matches their request.
            
            If the query is unclear or too vague, ask for clarification about what kind of movies they like (genre, mood, similar movies, etc.).
            """;

        try {
            PromptTemplate template = new PromptTemplate(promptTemplate);
            Prompt prompt = template.create(Map.of("query", userQuery));

            ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            return "Sorry, I'm having trouble connecting to the AI service right now. Please try again later.";
        }
    }
}