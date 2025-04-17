package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class AISuggestionDTO {

    private String suggestion;

    public AISuggestionDTO() {
    }

    public AISuggestionDTO(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return "AISuggestionDTO{" +
                "suggestion='" + suggestion + '\'' +
                '}';
    }
}
