import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class WebSocketService {
  constructor() {
    this.client = null
    this.connected = false
    this.subscribers = new Map() // topic -> Array of callback functions
    this.activeSubscriptions = new Map() // topic -> Stomp Subscription Object
    this.username = null
  }

  connect(username) {
    if (this.connected && this.username === username) return
    
    this.disconnect()
    this.username = username

    // Initialize STOMP client using SockJS fallback mechanism
    let wsUrl = process.env.VUE_APP_WS_URL || 'http://localhost:8080/ws'
    if (wsUrl.startsWith('ws://')) {
      wsUrl = wsUrl.replace('ws://', 'http://')
    } else if (wsUrl.startsWith('wss://')) {
      wsUrl = wsUrl.replace('wss://', 'https://')
    }
    const socket = new SockJS(wsUrl)
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    })

    this.client.onConnect = (frame) => {
      this.connected = true
      console.log('WebSocket Connected Successfully: ' + frame)
      
      // Subscribe all unique topics to client
      this.subscribers.forEach((callbacks, topic) => {
        this._subscribeToClient(topic)
      })
    }

    this.client.onStompError = (frame) => {
      console.error('Broker error: ' + frame.headers['message'])
      console.error('Details: ' + frame.body)
    }

    this.client.onWebSocketClose = () => {
       this.connected = false
       // Invalidate subscriptions on close
       this.activeSubscriptions.clear()
    }

    this.client.activate()
  }

  _subscribeToClient(topic) {
    if (!this.client || !this.connected) return
    // Avoid duplicate actual subscription calls to the broker
    if (this.activeSubscriptions.has(topic)) return
    
    const sub = this.client.subscribe(topic, (message) => {
      if (message.body) {
        const payload = JSON.parse(message.body)
        const callbacks = this.subscribers.get(topic) || []
        callbacks.forEach(cb => cb(payload))
      }
    })
    
    this.activeSubscriptions.set(topic, sub)
  }

  subscribe(topic, callback) {
    if (!this.subscribers.has(topic)) {
      this.subscribers.set(topic, [])
    }
    
    this.subscribers.get(topic).push(callback)
    
    // If already connected, ensure underlying subscription is pushed only once
    if (this.connected) {
      this._subscribeToClient(topic)
    }
    
    // Return unsubsribe helper function
    return () => this.unsubscribe(topic, callback)
  }

  unsubscribe(topic, callback) {
    if (!this.subscribers.has(topic)) return
    
    const callbacks = this.subscribers.get(topic)
    const index = callbacks.indexOf(callback)
    if (index > -1) {
      callbacks.splice(index, 1)
    }
    
    // If no more application listeners, remove actual broker subscription
    if (callbacks.length === 0) {
      this.subscribers.delete(topic)
      const activeSub = this.activeSubscriptions.get(topic)
      if (activeSub) {
        activeSub.unsubscribe()
        this.activeSubscriptions.delete(topic)
      }
    }
  }

  // Specifically shorthand for general notifications
  subscribeToNotifications(userId, callback) {
    const topic = `/topic/notifications/${userId}`
    return this.subscribe(topic, callback)
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
    this.connected = false
    this.subscribers.clear()
    this.activeSubscriptions.clear()
    this.username = null
  }
}

const instance = new WebSocketService()
export default instance
