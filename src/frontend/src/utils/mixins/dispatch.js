export default {
  methods: {
    dispatch(componentName, eventName, params) {
      let parent = this.$parent || this.$root;

      while (parent) {
        if (parent.$options.name === componentName) {
          parent.$emit(eventName, params);
          return parent;
        }

        parent = parent.$parent;
      }
    }
  }
}