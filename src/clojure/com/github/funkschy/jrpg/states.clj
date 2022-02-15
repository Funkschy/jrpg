(ns com.github.funkschy.jrpg.states)

(defn- next-state [state-machine args]
  (apply (:transition-fn state-machine) (:current state-machine) args))

(defn update-state-machine [state-machine & args]
  (assoc state-machine :current (next-state state-machine args)))

(defn current-state-data [state-machine]
  ((:states state-machine) (:current state-machine)))

(defn state-change? [state-machine args]
  (not= (next-state state-machine args)
        (:current state-machine)))

