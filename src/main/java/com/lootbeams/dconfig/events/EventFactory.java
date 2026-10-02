package com.lootbeams.dconfig.events;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class EventFactory {
   public EventFactory() {
   }

   public static <T> EventFactory.Event<T> createArrayBacked(Class<? super T> type, EventFactory.EventInvokerFactory<T> invokerFactory) {
      return new EventFactory.Event<>(type, invokerFactory);
   }

   public static class Event<T> {
      private final List<T> listeners = new ArrayList<>();
      private final EventFactory.EventInvokerFactory<T> invokerFactory;
      private final Class<?> listenerClass;
      private T invoker;

      public Event(Class<? super T> listenerClass, EventFactory.EventInvokerFactory<T> invokerFactory) {
         this.listenerClass = listenerClass;
         this.invokerFactory = invokerFactory;
         this.updateInvoker();
      }

      public T invoker() {
         return this.invoker;
      }

      public void register(T listener) {
         this.listeners.add(listener);
         this.updateInvoker();
      }

      private void updateInvoker() {
         T[] listenerArray = (T[])((Object[])Array.newInstance(this.listenerClass, this.listeners.size()));
         this.invoker = this.invokerFactory.create((T[])this.listeners.toArray(listenerArray));
      }
   }

   @FunctionalInterface
   public interface EventInvokerFactory<T> {
      T create(T[] var1);
   }
}
