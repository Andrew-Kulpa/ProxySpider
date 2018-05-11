/**
* @author Andrew Kulpa & Darren Wolbers
* @since May. 1, 2018
* @version 1.0
*/
package spiderproxy;
public class SpiderProxy {
    public static void main(String[] args) {
        int port;
        if(args.length < 1){
            System.out.println("No port defined, by default using 8080");
            port = 8080;
        } else{ // port hopefully defines
            try{
                port = Integer.parseInt(args[0]);
            }catch(NumberFormatException e){
                System.out.println("Improper port '" + args[0] +"' used, by default using 8080");
                port = 8080;
            }
        }
        
        Proxy.setup(port);
    }
}
