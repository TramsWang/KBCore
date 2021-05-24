gender(X0,male):-father(X0,?)
gender(X0,female):-mother(X0,?)
parent(X0,X1):-mother(X0,X1)
parent(X0,X1):-father(X0,X1)