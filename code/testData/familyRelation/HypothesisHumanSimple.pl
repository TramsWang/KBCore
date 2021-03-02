gender(X,male):-father(X,Y).
gender(X,female):-mother(X,Y).
parent(X,Y):-father(X,Y).
parent(X,Y):-mother(X,Y).