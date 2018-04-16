(ns init.index
  (:refer-clojure :exclude [atom])
  (:require [freactive.core :refer [atom]]
            [freactive.dom :as dom]
            #_[clojure.string :as s]
            [haslett.client :as ws]
            [haslett.format :as fmt]
            [cljs.core.async :refer [put! take! chan <! >!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [freactive.macros :refer [rx]]))

(defonce stream (atom nil))

(defn log1 [& args] (js/console.log (pr-str args)))

(defn recv-msg [stream]
  (let [{:keys [source]} @stream]
    (go-loop [i 0]
      (let [m (<! source)
            [h1 & r1] m
            ch-key (:%c h1)
            ch (get @stream ch-key)
            rtn (>! ch m)]

        (prn :recv i rtn h1 r1)

        (when m (recur (inc i)))))))

(defn req [stream h1 r1]
  (let [{:keys [source sink]} @stream
        ch-key (gensym "c")
        ch (chan)]
    (swap! stream assoc ch-key ch)
    (log1 :c (keys @stream))
    (go
      (>! sink (cons (merge {:%c ch-key} h1) r1))
      (let [rtn (<! ch)]
        (swap! stream dissoc ch-key)
        (log1 :resp rtn)
        rtn))))

;; ------------------------- 
;; Views

(defn view [last-msg]
  [:div
   #_[:h3 "Websocket by cljs"]
   [:button
    {:on-click
     (fn [e]
       (req stream {:f 'f1} (list 1 2 3))
       )
     }
    "Click me!"]
   ])

;; -------------------------
;; Initialize app

(set!
 (.-onload js/window)
 (fn []
   (let [root
         (dom/append-child! (.-body js/document) [:div#root])]

     (aset js/document "title" "Websocket by cljs")

     (dom/mount! root (view last-msg))
     (log1 "page has been mounted")

     (go
       (reset!
        stream
        (<! (ws/connect
             "ws://echo.websocket.org"
             {:format fmt/transit}
             )))

       (log1 "ws is connected")
       (recv-msg stream)
       (req stream {:f 'f1-start} (list 1 2 3))
       #_(ws/close @stream)
       ))))
