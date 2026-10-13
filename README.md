# Persistent Reading List

## Background
The repo **reading-list** (Part I) implements a working, tested reading list that forgets everything when you reload the page. Part II fixes that in two ways: 
- Datalog storage inside the browser persisting to localstorage, 
- Clojure server and a Datomic database, and lets the user choose between them. 


## How persistence works
All of Part I's state lives in one atom, and an atom lives in the memory of the browser tab. Reloading the page throws that memory away, runs init again, and creates a fresh atom from initial-state. Nothing was ever written anywhere that outlives the page.

Persistence therefore means two things: 
1. Writing the data somewhere durable whenever it changes, and 
2. Rading it back when the app starts. 

Everything in Part II is a variation on those two moves. The interesting questions are what to write, where to write it, and how to keep the rest of the app from caring.


## The application state
Looking at what store held in Part I there us the followinf state: 
- The *books* held in the :books key
- The *partial text* being typed (:draft) 
- The chosen *filter* (:show)

**NOTE**: Only the books are worth saving. Reloading and finding your half-typed title still there would be odd, and saving every keystroke to a server would be wasteful.


