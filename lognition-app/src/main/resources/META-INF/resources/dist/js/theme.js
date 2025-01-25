"use strict";

// Dark theme info from https://brandur.org/fragments/dark-mode-notes and
// https://css-tricks.com/a-complete-guide-to-dark-mode-on-the-web/.

// TODO: also need to update theme when in automode and system theme changes.

const THEME_AUTO = "auto";
const THEME_DARK = "dark";
const THEME_LIGHT = "light";
const THEMES = Object.freeze([THEME_AUTO, THEME_DARK, THEME_LIGHT]);

function getStoredTheme() {
    return localStorage.getItem("theme") || THEME_AUTO;
}

function storeTheme(theme) {
    if (theme !== localStorage.getItem("theme")) {
        localStorage.setItem("theme", theme);
    }
}

function applyTheme(theme) {
    let themeClass = theme;
    if (theme === THEME_AUTO && window.matchMedia) {
        if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
            themeClass = THEME_DARK;
        } else if (window.matchMedia('(prefers-color-scheme: light)').matches) {
            themeClass = THEME_LIGHT;
        }
    }
    const docClassList = document.documentElement.classList;
    THEMES.forEach((th) => {
        docClassList.remove(th);
        docClassList.remove(th + "-theme");
    });
    docClassList.add(themeClass);
    docClassList.add(theme + "-theme");
    document.querySelector("meta[name=color-scheme]")?.setAttribute("content", themeClass);

    storeTheme(theme);
}

applyTheme(getStoredTheme());

// Listen to stored theme change, allows other tabs to change at the same time.
window.addEventListener("storage", (e) => {
        if (e.key === "theme") {
            applyTheme(getStoredTheme());
        }
    });

// Listen to OS-theme change so that auto theme can match.
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change',
    (e) => {
        applyTheme(getStoredTheme())
    });