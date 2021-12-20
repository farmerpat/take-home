(ns take-home.core
  (:require
    [day8.re-frame.http-fx]
    [cljs-time.core :as tc]
    [cljs-time.format :as tf]
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

(def repo-name (r/atom ""))

(defn text-input []
  [:input {:id "repo_input"
           :type "text"
           :value @repo-name
           :on-change #(reset! repo-name (.-value (.-target %)))
           :placeholder "Repo"}])

(defn repo-add-button []
  [:button
   {:id "repo_search_submit"
    :on-click (fn [e]
                (.preventDefault e)
                (rf/dispatch [:submit-search @repo-name]))}
   "Add"])

(defn format-time [t]
  (let [formatter (tf/formatter "yyyy-MM-dd HH:mm")]
    (tf/unparse formatter t)))

;; TODO
;; Fixed by dereferencing subscription outside of let...?
;; Find out why.
(defn repo-list []
  (let [repos (rf/subscribe [:repo-list])]
    (fn []
      [:div {:id "repos_container"}
       [:div#repo_list_header
        [:h1 "Repo List"]]
       [:div {:id "repos_list"}
        (map (fn [repo]
               (let [class (if (:seen repo) "repo_entry" "repo_entry unseen")
                     release-date (format-time (:release-date repo))]
                 ;; TODO
                 ;; make a view that generates this madness.
                 [:div {:class "repo_entry_container"}
                  [:div {:class class
                         :on-click (fn [e]
                                     (.preventDefault e)
                                     (rf/dispatch [:repo-view (:name repo)]))}
                   [:div {:class "repo_name_container"}
                    (str (:owner repo) "/" (:name repo))]
                   [:div {:class "repo_time_container"}
                    [:div {:class "repo_time"} release-date]]
                   ]
                  [:div {:class "repo_remove_container"
                         :on-click (fn [e]
                                     (.preventDefault e)
                                     (rf/dispatch [:repo-remove (:name repo)]))} "X"]]))
             @repos)]])))

(defn repo-details-container []
  (let [repos (rf/subscribe [:repo-list])]
    (fn []
      (let [matches (filter #(:selected-for-detail-view %) @repos)
            release-notes (:release-notes (first matches))]
        [:div {:class "repos_detail_container"}
         [repo-list]
         [:div#repo_release_notes_container
          {:dangerouslySetInnerHTML {:__html (md->html release-notes)}}]]))))

(defn home-page []
  [:section.section>div.container>div.content
   [:div#repo_search_refresh_container
    [:div#repo_search_form_container
     [:form#repo_search_form
      [:label {:for "repo_input"} "Repo:"]
      [text-input]
      [repo-add-button]]]]
   [repo-details-container]])

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:init-db])
  (ajax/load-interceptors!)
  (mount-components))
