import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  DEFAULT_LANGUAGE,
  LANGUAGE_STORAGE_KEY,
  translations,
} from "./translations.js";

const I18nContext = createContext(null);

function getInitialLanguage() {
  const stored =
    typeof window !== "undefined"
      ? window.localStorage.getItem(LANGUAGE_STORAGE_KEY)
      : null;
  return translations[stored] ? stored : DEFAULT_LANGUAGE;
}

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(getInitialLanguage);

  useEffect(() => {
    document.documentElement.setAttribute("lang", lang);
  }, [lang]);

  const setLang = useCallback((nextLang) => {
    if (!translations[nextLang]) return;
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, nextLang);
    setLangState(nextLang);
    document.documentElement.setAttribute("lang", nextLang);
  }, []);

  const t = useCallback(
    (key, fallback) => {
      const dictionary = translations[lang] || translations[DEFAULT_LANGUAGE];
      if (dictionary && dictionary[key] !== undefined) {
        return dictionary[key];
      }
      return fallback !== undefined ? fallback : key;
    },
    [lang]
  );

  const value = useMemo(() => ({ lang, setLang, t }), [lang, setLang, t]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used within an I18nProvider");
  }
  return context;
}
