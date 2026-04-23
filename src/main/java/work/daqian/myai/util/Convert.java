package work.daqian.myai.util;

@FunctionalInterface
public interface Convert<R,T>{
    void convert(R origin, T target);
}
