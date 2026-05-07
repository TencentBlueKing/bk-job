import { ref } from 'vue';

import { useRoute } from '@router';

import AiService from '@service/ai';


export const useCurrentSessionCode = () => {
  const currentSessionCode = ref(null);


  const route = useRoute();

  console.log('route = ', route.value);


  if (route.value.name === 'historyStep') {
    // AiService.fetchChatSession({
    //   sceneType: 1,
    //   sceneResourceId: route.query.stepInstanceId,
    // });
  }

  const getCurrentSessionCode = async () => {
    if (route.value.name === 'historyStep') {
      return AiService.fetchChatSession({
        sceneType: 1,
        sceneResourceId: route.value.query.stepInstanceId,
      });
    }
  };
  return { currentSessionCode, getCurrentSessionCode };
};
