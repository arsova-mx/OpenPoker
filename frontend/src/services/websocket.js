import SockJS from "sockjs-client";
import { Stomp } from "@stomp/stompjs";

export function connectToSession(sessionId, inviteCode, username) {

  const socket = new SockJS("/ws");
  const stomp = Stomp.over(socket);

  stomp.connect({}, () => {

    // Escuchar participantes
    stomp.subscribe(`/topic/session/${inviteCode}/participants`, (msg) => {
      const participants = JSON.parse(msg.body);
      console.log("Participantes:", participants);
    });

    // Unirse a la session
    stomp.send("/app/session.join", {}, JSON.stringify({
      inviteCode,
      username
    }));
  });

  return stomp;
}