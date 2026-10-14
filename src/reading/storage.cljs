(ns reading.storage
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [datascript.core :as d]
            [reading.domain :as domain]))

(defprotocol Storage
  (load-books [this]
    "A promise of a vector of book maps.")
  (persist! [this commands db]
    "Save, after commands were applied to db in memory.
    Returns a promise."))

;; ============================================================
;; In this browser: localStorage
;; ============================================================
(def books-key "reading-list/books")

(defn serialize [books]
  (pr-str books))

(defn deserialize [s]
  (if (str/blank? s)
    []
    (reader/read-string s)))

(defrecord LocalStorage []
  Storage
  
  (load-books [_]
    ;; js/Promise. turns an exception (say, corrupt data) into a rejection
    (js/Promise.
     (fn [resolve _]
       (resolve (deserialize (.getItem js/localStorage books-key))))))
  
  (persist! [_ _commands db]
    (.setItem js/localStorage books-key (serialize (domain/books d/q db)))
    (js/Promise.resolve true)))

;; ============================================================
;; Choosing
;; ============================================================
(def mode-key "reading-list/storage-mode")

(defn saved-mode []
  (if (= "server" (.getItem js/localStorage mode-key)) :server :local))

(defn remember-mode! [mode]
  (.setItem js/localStorage mode-key (name mode)))

(defn storage-for
  "The storage for a mode. For now there is only this browser;
  step 20 adds the server."
  [_mode]
  (->LocalStorage))
