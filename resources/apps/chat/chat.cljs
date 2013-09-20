(ns demo.chat
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]
    [clang.angular :refer [def.controller defn.scope def.filter in-scope s-set fnj]]
    [tailrecursion.javelin.macros :refer [cell]])
  (:use [clang.util :only [? module]])
  (:require [plugh.core :as pc]
            [cljs.core.async :as async
             :refer [<! >! chan]]
            tailrecursion.javelin))

(def server-chan (pc/server-chan "The Chat Server"))

(def compiler-chan (pc/server-chan "cljs compiler"))

;;; input cell
(def chats-cell (cell '[]))

;;; formula cell
(def shouts (cell (->> chats-cell
                       (mapv #(.toUpperCase %))
                       clj->js)))

(def.controller pc/m Chatter [$scope $compile]
  ;; output cell
  (cell (s-set :shouts shouts))
  
  (s-set :line "")
  
  (defn.scope send [] 
    (let [msg (:line $scope)]
      (go 
        (>! server-chan {:msg msg})))
    (assoc! $scope :line ""))
  
  
  (let [rc (chan)]
    (go (>! server-chan {:add rc}))
    (letfn [(proc [] 
                  (go (let [chats (<! rc)]
                        (doseq [m chats]
                          ;; mutate input cell
                          (swap! chats-cell conj m))
                        (proc))))]
      (proc)))
  )
    
    
