export default (aiRef, handleRef, callback = () => {}) => {
  const animateTimes = 400;
  return () => {
    const sourceEle = aiRef.value.$el.querySelector('.ai-modal');
    const sourceEleClone = sourceEle.cloneNode(true);
    const {
      bottom: sourceBottom,
      right: sourceRight,
      width: sourceWidth,
      height: sourceHeight,
    } = sourceEle.getBoundingClientRect();
    const styles = sourceEleClone.style;
    styles.position = 'fixed';
    styles.bottom = `${sourceRight}px`;
    styles.right = `${sourceRight}px`;
    styles.width = `${sourceWidth}px`;
    styles.height = `${sourceHeight}px`;
    styles.opacity = 1;
    styles.zIndex = 999999999;
    document.body.appendChild(sourceEleClone);
    setTimeout(() => {
      const {
        bottom: targetBottom,
        height: targetHeight,
      } = handleRef.value.getBoundingClientRect();

      const translateX = window.innerWidth - sourceRight;
      const translateY = targetBottom - targetHeight / 2 - sourceBottom;
      styles.opacity = 0.3;
      styles.overflow = 'hidden';
      styles.transform = `translate(${translateX}px, ${translateY}px) scale(0.01)`;
      styles.transition = `transform ${animateTimes / 1000}s cubic-bezier(0.74, 0.01, 0.2, 1)`;
      styles.transformOrigin = 'right bottom';
      setTimeout(() => {
        document.body.removeChild(sourceEleClone);

        callback();
      }, animateTimes);
    });
  };
};
