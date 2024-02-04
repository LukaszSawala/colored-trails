package View;

import Model.Color;
import Model.Token;

import javax.swing.*;

public class TokenButton extends JButton {
    private Token token;
    public TokenButton(Token token) {
        super();
        this.token = token;
        this.setBackground(Color.getColor(token.getColor()));
    }
}
