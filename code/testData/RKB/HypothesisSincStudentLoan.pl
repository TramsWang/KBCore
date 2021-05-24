male(X0):-unemployed(X0)
person(X0):-no_payment_due(X0,?)
no_payment_due(X0,pos):-enrolled(X0,?,?)