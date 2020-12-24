package common;

import java.util.Collection;

public class Rule {
    public final Predicate head;
    public final Predicate[] body;

    public Rule(Predicate head, Collection<Predicate> body) {
        this.head = head;
        this.body = body.toArray(new Predicate[0]);
    }

    public Rule(Predicate head, Predicate[] body) {
        this.head = head;
        this.body = body;
    }

    public Rule(Rule another) {
        this.head = new Predicate(another.head);
        this.body = new Predicate[another.body.length];
        for (int i = 0; i < this.body.length; i++) {
            this.body[i] = new Predicate(another.body[i]);
        }
    }
}
