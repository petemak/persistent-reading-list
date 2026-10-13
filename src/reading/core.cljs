(ns reading.core
  (:require [replicant.dom :as r]
            [reading.ui :as ui]
            [reading.state :as state]
            [reading.actions :as actions]))

;; The one atom. defonce keeps its value across hot reloads.
(defonce store (atom state/initial-state))

;;
(defn render!
  [state]
  (r/render (js/document.getElementById "app")
            (ui/render-app state)))

;; shadow-cljs calls this after every hot reload
(defn ^:dev/after-load reload!
  []
  (render! @store))

;; The smallest possible Replicant program: one call to render.
(defn init
  []
  (r/set-dispatch!
   (fn [{:replicant/keys [trigger dom-event]} action-data]
     (when (= trigger :replicant.trigger/dom-event)
       (actions/handle! store dom-event action-data))))

  (add-watch store
             ::render
             (fn [_ _ _ new-state]
               (render! new-state)))
  
  (render! @store))
