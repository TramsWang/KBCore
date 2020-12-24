package common;

import org.jpl7.Compound;

import java.util.Arrays;

public class JplRule {
    public Compound head;
    public Compound[] body;

    public JplRule(Compound head, Compound[] body) {
        this.head = head;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(head.toString());
        builder.append(":-");
        if (0 < body.length) {
            builder.append(body[0].toString());
            for (int i = 1; i < body.length; i++) {
                builder.append(',').append(body[i].toString());
            }
        }
        return builder.toString();
    }
}
