(ns reading.test-util
  (:require  [cljs.test :as t :include-macros true]))

(defn nodes
  "Every hiccup element in a tree, depth first, including the root."
  [hiccup]
  (filter #(and (vector? %) (keyword? (first %)))
          (tree-seq sequential? seq hiccup)))


(defn attrs
  "The attribute map of an element, or nil."
  [node]
  (let [a (second node)]
    (when (map? a) a)))


(defn tag-name
  "The bare tag: :li.read#x becomes \"li\"."
  [node]
  (re-find #"^[^.#]+" (name (first node))))


(defn find-tag [hiccup tag]
  (filter #(= tag (tag-name %)) (nodes hiccup)))


(defn text
  "All the text inside an element, joined."
  [node]
  (apply str (filter #(or (string? %) (number? %)) (tree-seq sequential? seq node))))
