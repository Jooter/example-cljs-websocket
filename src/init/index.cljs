(ns init.index
  (:refer-clojure :exclude [atom])
  (:require [freactive.core :refer [atom]]
            [freactive.dom :as dom]
            #_[clojure.string :as s]
            [haslett.client :as ws]
            #_[haslett.format :as fmt]
            [cljs.core.async :refer [put! take! chan <! >!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [freactive.macros :refer [rx]]))

(defn log1 [& args] (js/console.log (pr-str args)))

(defn send-msg [{:keys [sink]}]
  (put! sink "Hello World 1")
  (put! sink "Hello World 2"))

(defn recv-msg [stream last-msg]
  (go-loop [i 0]
    (let [m (<! (:source stream))]
      (reset! last-msg m)
      (prn :recv i m)
      (when m (recur (inc i))))))

;; ------------------------- 
;; Views

(defn view [last-msg]
  [:div
   [:h3 "Websocket by cljs"]
   (rx
    (if @last-msg
      [:div
       [:p "Last message::"]
       [:p @last-msg]
       ]))
   ])

;; -------------------------
;; Initialize app

(set!
 (.-onload js/window)
 (fn []
   (let [last-msg (atom nil)
         root
         (dom/append-child! (.-body js/document) [:div#root])]

     (aset js/document "title" "Websocket by cljs")

     (dom/mount! root (view last-msg))

     (go
       (let [stream
             (<! (ws/connect
                  "ws://echo.websocket.org"
                  {:source (chan 1 (map #(str "svr: " %)))}
                  ))]
           ; (>! (:sink stream) "Hello World")
           #_(prn :recv (<! (:source stream)))

           ; (send-msg (:sink   stream))
           ; (recv-msg (:source stream))
           (recv-msg stream last-msg)
           (send-msg stream)
           #_(ws/close stream)
           ))

     (log1 "page has been mounted")
     )))
