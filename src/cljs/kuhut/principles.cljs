(ns kuhut.principles
  (:require [reagent.session :as session]
            [accountant.core :as accountant]
            [kuhut.shared.definitions :refer [PRINCIPLES_LIFELONG_RESOURCE_ID
                                              PRINCIPLES_SAFE_RESOURCE_ID PRINCIPLES_HELPOTHERS_RESOURCE_ID
                                              PRINCIPLES_AI_RESOURCE_ID PRINCIPLES_EQUALITY_RESOURCE_ID]]
            [kuhut.router :refer [path-for]]
            [kuhut.util :refer [contained scroll-to]])
  )

(set! *warn-on-infer* true)

(defn principles-page
  [user]
  [:div.principles-page
   [:div#lifelong.principle.left
    [:div.principle-content
     [:div.principle-title "1. Learning is a " [:div.darker "life long"] " process"]
     [:div.principle-message
      "Found some new interests?" [:br]
      "Decided to take up a new career?" [:br]
      "Studying for school?" [:br]
      "Just curious about the world around you?" [:br] [:br]
      "The kuhut " [:em [:div.darker "Learning Intelligence System™"]] " is always up to date, and will guide you through the most effective and enjoyable path for learning new things!"]]
    [:div.principle-image
     [:img {:src (str "/resource/" PRINCIPLES_LIFELONG_RESOURCE_ID)}]]]
   [:div#safe.principle.right
    [:div.principle-content
     [:div.principle-title "2. It's OK to " [:div.darker "make mistakes"] " in a safe learning environment"]
     [:div.principle-message
      "The kuhut system has been designed with a single purpose in mind - to help you learn as effectively and as quickly as possible. You wouldn't be here if you knew everything, so on that rare occasion when you do make a mistake, we will do anything to help you understand the topic better!"]]
    [:div.principle-image
     [:img {:src (str "/resource/" PRINCIPLES_SAFE_RESOURCE_ID)}]]]
   [:div#helpothers.principle.left
    [:div.principle-content
     [:div.principle-title "3. " [:div.darker "Help others"] " learn"]
     [:div.principle-message
      "By simply using kuhut, you could be helping people from around the world learn as well." [:br] [:br] [:div.darker "How it works:"] " some people have been blessed with exceptionally innovative ways of learning new things. If you'd like to share that, and lend a hand to others who are less fortunate, we at kuhut will make this possible!"]]
    [:div.principle-image
     [:img {:src (str "/resource/" PRINCIPLES_HELPOTHERS_RESOURCE_ID)}]]]
   [:div#ai.principle.right
    [:div.principle-content
     [:div.principle-title "4. Your " [:div.darker "personal AI"] " learning assistant"]
     [:div.principle-message
      "We all learn in different ways. Some of us learn by listening, some prefer reading, and others learn by writing or presenting. Whatever your preferred approach, having a personal learning assistant means that your skills will be nurtured and developed in the way that fits you best."]]
    [:div.principle-image
     [:img {:src (str "/resource/" PRINCIPLES_AI_RESOURCE_ID)}]]]
   [:div#equality.principle.center
    [:div.principle-content
     [:div.principle-title "5. A truly " [:div.darker "equal opportunity"] " world"]
     [:div.principle-message
      [:center "Our most important founding principle is that " [:div.darker "elementary education is a basic human right"] "." [:sup "1"]] [:br] [:em "With that in mind, we make a promise:"] [:br] "While professional education can be funded privately, all of kuhut's learning journeys, however successful now or in the future, that are either "
      [:em [:div.darker "beginner level"]]
      " or "
      [:em [:div.darker "designed for children"]]
      " of any age, and our best in class learning AI that powers them, are completely " [:div.darker "free of charge"] "." [:br] "Forever."]]
    [:div.principle-image
     [:img {:src (str "/resource/" PRINCIPLES_EQUALITY_RESOURCE_ID)}]]]
   [:div#more
    [:sup "1"] "Article 26 of " [:div.linked-reference.external {:on-click #(do (.open js/window "/human-rights.pdf") false)} "The Universal Declaration of Human Rights"]
    [:sup " [opens a PDF document downloaded from "
     [:div.linked-reference.external {:on-click #(do (.open js/window "https://www.un.org/") false)} "https://www.un.org/"] " in a separate tab/window]"]
    [:br] [:br] [:br]
    (if (-> :user session/get :first-name)
      [:div.linked-reference.membership {:on-click #(do
                                               (session/remove! :overlay)
                                               (accountant/navigate! (path-for :home)))} "continue to learning"]
      [:div
       [:div.linked-reference.membership {:on-click #(contained % (session/put! :overlay :menu))} "sign up"]
       " or simply "
       [:div.linked-reference.membership {:on-click #(accountant/navigate! (path-for :home))} "start learning"]])]]
  )
