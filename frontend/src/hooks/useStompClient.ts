import { useEffect, useRef, useState, useCallback } from "react";
import { Client, IMessage, StompSubscription } from "@stomp/stompjs";

// Función utilitaria para enmascarar tokens JWT en las cadenas de log
function sanitizeStompLog(message: string): string {
  return message.replace(
    /(Authorization:\s*(?:Bearer\s+)?)[^\r\n]+/gi,
    "$1[REDACTED]"
  );
}

export function useStompClient(token?: string | null) {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const brokerURL = `${protocol}//${window.location.host}/ws-native`;

    const headers: Record<string, string> = {};
    if (token) {
      headers["Authorization"] = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
    }

    const client = new Client({
      brokerURL,
      connectHeaders: headers,
      // Solo loguea si estamos en desarrollo y oculta la cabecera Authorization
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log("[STOMP]", sanitizeStompLog(str));
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnected(true);
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onStompError: (frame) => {
        if (import.meta.env.DEV) {
          console.error("STOMP error:", frame.headers["message"]);
        }
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [token]);

  const subscribe = useCallback(
    (destination: string, callback: (body: any) => void): StompSubscription | undefined => {
      if (!clientRef.current || !clientRef.current.connected) return undefined;

      return clientRef.current.subscribe(destination, (msg: IMessage) => {
        try {
          callback(JSON.parse(msg.body));
        } catch {
          callback(msg.body);
        }
      });
    },
    []
  );

  const publish = useCallback((destination: string, body: object) => {
    if (!clientRef.current || !clientRef.current.connected) return;

    clientRef.current.publish({
      destination,
      body: JSON.stringify(body),
      headers: { "content-type": "application/json" },
    });
  }, []);

  return { connected, subscribe, publish };
}

export default useStompClient;