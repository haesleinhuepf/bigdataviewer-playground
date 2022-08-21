package sc.fiji.bdvpg.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ij.IJ;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalCache {

    public GlobalCache() {

        new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                IJ.log("Cache estimated size : "+cache.estimatedSize());
            }
        }).start();

    }

    final Cache< Key, Object > cache = Caffeine.newBuilder()
            .maximumSize( 100 )
            .softValues()
            .build();

    void touch( final Key key ) {
        cache.getIfPresent(key); // Touch
    }

    static public Key getKey(Object source, int timepoint, int level, Object key){
        return new Key(source, timepoint, level, key);
    }

    public void put(Key key, Object value) {
        cache.put(key, value);
    }

    public static class Key
    {
        private final WeakReference<Object> source;

        private final int timepoint;

        private final int level;

        private final WeakReference<Object> key;


        public Key( final Object source, final int timepoint, final int level, final Object key )
        {
            this.source = new WeakReference<>(source);
            this.timepoint = timepoint;
            this.level = level;
            this.key = new WeakReference<>(key);

            int value = source.hashCode();
            value = 31 * value + level;
            value = 31 * value + key.hashCode();
            value = 31 * value + timepoint;
            hashcode = value;
        }

        @Override
        public boolean equals( final Object other )
        {
            if (source.get()==null) return false;
            if (key.get()==null) return false;

            if ( this == other )
                return true;
            if ( !( other instanceof GlobalCache.Key ) )
                return false;
            final GlobalCache.Key that = (GlobalCache.Key) other;

            return ( this.source.get() == that.source.get() )
                    && ( this.timepoint == that.timepoint )
                    && ( this.level == that.level )
                    && ( this.key.get().equals(that.key.get()) );
        }

        final int hashcode;

        @Override
        public int hashCode()
        {
            return hashcode;
        }
    }

}
