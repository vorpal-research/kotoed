declare module "twemoji" {
    public interface Twemoji {
        parse(x: any): string;
    };

    declare const twemoji: Twemoji;
    export default twemoji;
}