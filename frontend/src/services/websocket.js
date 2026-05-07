import SockJS from "sockjs-client";
import { Stomp } from "@stomp/stompjs";

export function connectToSession(sessionId, inviteCode, displayName) {

  const socket = new SockJS("http://localhost:8080/ws");
  const stomp = Stomp.over(socket);

  stomp.connect({}, () => {

    // Escuchar participantes
    stomp.subscribe(`/topic/session/${sessionId}/participants`, (msg) => {
      const participants = JSON.parse(msg.body);
      console.log("Participantes:", participants);
    });

    // Unirse a la session
    stomp.send("/app/session.join", {}, JSON.stringify({
      inviteCode,
      displayName
    }));
  });

  return stomp;
}