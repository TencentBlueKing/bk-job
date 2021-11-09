export const calcTextWidth = (text, parentEl = document.body) => {
    const $el = document.createElement('div');
    $el.innerText = text;
    $el.style.position = 'absolute';
    $el.style.opacity = 0;
    $el.style.zIndex = '-1';
    $el.style.whiteSpace = 'pre';
    $el.style.wordBreak = 'no-break';
    parentEl.appendChild($el);
    const { width } = $el.getBoundingClientRect();
    parentEl.removeChild($el);

    return width;
};
