(ns take-home.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [take-home.ajax :as ajax]
    [take-home.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "take-home"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]]]]))

(def repo-name (r/atom ""))

(defn text-input [id]
  [:div.repo-input
   [:label "Repo:"]
   [:input {:type "text"
            :value @repo-name
            :on-change #(reset! repo-name (.-value (.-target %)))
            :placeholder "Repo"}]
   [:button
    {:on-click (fn [e]
                 (.preventDefault e)
                 (rf/dispatch [:submit-search @repo-name]))}
    "Search"]])

(defn repo-list []
  (let [repos @(rf/subscribe [:home/repo-list])]
    (fn []
      ;; then foreach repo add it to the div or what not
      (println "i r repo-list with repos")
      [:div
       [:h1 "Repo List"]
       [:ul
        (map (fn [{:keys [release-date name owner]}]
               [:li (str owner "/" name)])
             repos)]])))

(defn home-page []
  (println "i r home-page with repolists")
  [:section.section>div.container>div.content
   [text-input]
   [repo-list]])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:init-db])
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
