;; social-wallet - A generic social wallet UI which uses the social-wallet-api for a beckend

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.util
  #_(:require [midje.sweet :refer [against-background before after facts fact =>]]
            [social-wallet
             [util :as u]]
            [taoensso.timbre :as log]))

#_(facts "Check that the deep-merge fn is doing what it is supposed to do"
       (fact "check that the deep-merge overrides with the latter map the former when same keys"
             (let [former {:a {:a1 true
                               :a2 true}}
                   latter {:a {:a2 false}}]
               (u/deep-merge former latter) => {:a {:a1 true
                                                    :a2 false}}))
       (fact "check that when new keys introduced in the latter they are added"
             (let [former {:a {:a1 true
                               :a2 true}}
                   latter {:a {:a2 false
                               :a3 true}}]
               (u/deep-merge former latter) => {:a {:a1 true
                                                    :a2 false
                                                    :a3 true}})))
