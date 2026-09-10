import { useEffect, useRef, useState, useCallback } from "react";
import { Client, IMessage } from "@stomp/stompjs";

export function useStompClient(token?: string | null) {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    // Determinamos protocolo según http/https
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const brokerURL = `${protocol}//${window.location.host}/ws-native`;

    const client = new Client({
      brokerURL,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str) => console.log("[STOMP]", str),
      reconnectDelay: 5000,
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error("STOMP Error:", frame),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [token]);

  const subscribe = useCallback((destination: string, callback: (body: any) => void) => {
    if (!clientRef.current || !clientRef.current.connected) return undefined;

    return clientRef.current.subscribe(destination, (msg: IMessage) => {
      try {
        callback(JSON.parse(msg.body));
      } catch {
        callback(msg.body);
      }
    });
  }, []);

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