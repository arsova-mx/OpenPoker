import { useEffect, useRef, useState, useCallback } from "react";
import { Client, IMessage, StompSubscription } from "@stomp/stompjs";

function sanitizeStompLog(message: string): string {
  return message.replace(
    /(Authorization:\s*(?:Bearer\s+)?)[^\r\n]+/gi,
    "$1[REDACTED]"
  );
}

function getBrokerURL(): string {
  // Si tienes definida una URL base de API en Vite (ej: VITE_API_BASE_URL="http://localhost:8080/api")
  const apiUrl = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL;
  
  if (apiUrl) {
    try {
      const url = new URL(apiUrl, window.location.href);
      const protocol = url.protocol === "https:" ? "wss:" : "ws:";
      return `${protocol}//${url.host}/ws-native`;
    } catch {
      // Si la URL relativa o malformada falla, continúa al fallback estándar
    }
  }

  // Fallback por defecto usando el origen actual
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws-native`;
}

export function useStompClient(token?: string | null) {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const brokerURL = getBrokerURL();
    const headers: Record<string, string> = {};

    if (token) {
      headers["Authorization"] = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
    }

    const client = new Client({
      brokerURL,
      connectHeaders: headers,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log("[STOMP]", sanitizeStompLog(str));
        }
      },
      onConnect: () => {
        setConnected(true);
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onWebSocketClose: () => {
        setConnected(false);
      },
      onStompError: (frame) => {
        if (import.meta.env.DEV) {
          console.error("STOMP error:", frame.headers["message"]);
        }
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [token]);

  // Se implementa genérico <T = unknown> para eliminar la advertencia de lint @typescript-eslint/no-explicit-any
  const subscribe = useCallback(
    <T = unknown>(destination: string, callback: (body: T) => void): StompSubscription | undefined => {
      if (!clientRef.current || !clientRef.current.connected) return undefined;

      return clientRef.current.subscribe(destination, (msg: IMessage) => {
        try {
          const parsed = JSON.parse(msg.body) as T;
          callback(parsed);
        } catch {
          callback(msg.body as unknown as T);
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