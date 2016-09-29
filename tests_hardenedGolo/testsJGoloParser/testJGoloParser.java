import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.golo.compiler.jgoloparser.*;

public class testJGoloParser {
    private static int testNumber=0;
    private static final int TITLE_LENGTH = 20;
    private static boolean globalVerbose;

    private static void println(String s, boolean verbose) {
        if (verbose) {
            System.out.println(s);
        }
    }

    public static void makeTest(String title, String formula) {
        makeTest(title, formula, "");
    }

    public static void makeTest(String title, String formula, String expectedRes) {
        boolean verbose = globalVerbose;
        while(title.length()<TITLE_LENGTH) { title=title+" "; }
        if(expectedRes==null || expectedRes.equals("")) {
            verbose = true;
        }
        try {
            testNumber++;
            String testNbStr = ""+testNumber;
            while (testNbStr.length()<3) {
                testNbStr=" "+testNbStr;
            }
            JGSpecs specs = FolJGoloParser.parse(formula);
            String raw = specs.toString();
            String res = raw.replace("\n"," ").replace("\r"," ").replace("  "," ").trim();
            expectedRes = expectedRes.replace("\n"," ").replace("\r"," ").replace("  "," ").trim();

            boolean status=res.equals(expectedRes);

            // If verbose or FAIL
            verbose = verbose || !status;
            println("Test "+testNbStr+" ("+title+") : "+((status)?"ok":((expectedRes.equals(""))?"INCONCLUSIVE":"FAIL")), verbose);
            println("  Initial formula             : "+formula, verbose);
            println("  Interpreted as (raw)        : "+raw, verbose);
            println("                 (normalized) : "+res, verbose);
            println("", verbose);

            // If NOT verbose
            //  Title could be reduced only if no any problem occurs
            if(title.length()>TITLE_LENGTH) { title=title.substring(0,TITLE_LENGTH-3)+"..."; }
            println("Test "+testNbStr+" ("+title+") : "+((status)?"PASS":"FAIL"),!verbose);

        } catch(Exception e) {
            String testNbStr = ""+testNumber;
            while (testNbStr.length()<3) {
                testNbStr=" "+testNbStr;
            }
            println("Test "+testNbStr+" ("+title+") : FAIL", true);
            println("  Initial formula             : "+formula, true);
            println("", verbose);
            e.printStackTrace();
            System.err.println();
            System.err.println("Test process stopped");
            System.exit(0);
        }
    }
    public static void main(String[] a) {
        globalVerbose = false;
        if(a.length>0 && a[0].trim().equals("--verbose")) {
            globalVerbose = true;
        }

        System.out.println();
        makeTest("Trivial formula, with parametrized function", "ensures { p X Y } requires { p X }", "ensures { ( p X Y ) } requires { ( p X ) } ");
        makeTest("Existential formula", "ensures { exists X. (p) }", "ensures { exists X. ( p ) }");
        makeTest("Implies formula", "ensures { p -> q }", "ensures { ( p -> q ) }");
        makeTest("Universal formula", "ensures { forall X. (p X -> g X )}", "ensures { forall X. ( ( ( p X ) -> ( g X ) ) ) }");
        makeTest("Complex formula", "ensures {"+
             "forall X. (p X Y /\\ exists Y. (p Y X )) \n"+
             "-> (q c \\/ !r    ) }","ensures { ( forall X. ( (( p X Y ) /\\ exists Y. ( ( p Y X ) )) ) -> (( q c ) \\/ !r) ) }");

        makeTest("Unary minus", "ensures{\n x >= -2147483647 }","ensures { (x >= -2147483647) }");
        makeTest("Unary minus 1", "ensures{\n (result >= 0) /\\ \n (result = x \\/ result = x)} \n requires{ \n x >= 2147483647 \n }",
                                  "ensures { ((result >= 0) /\\ ((result = x) \\/ (result = x))) } requires { (x >= 2147483647) }");

        makeTest("Unary minus 2", "ensures{result = -x} ","ensures { (result = ( -x ) ) }");
        makeTest("Unary minus 3", "ensures{\n (result = x \\/ (result = -x))} ", "ensures { ((result = x) \\/ (result = ( -x ) )) }");

        makeTest("Inequation formula", "ensures{\n (result >= 0) /\\ \n (result = x \\/ result = -x)} \n requires{ \n x >= -2147483647 \n }",
                                  "ensures { ((result >= 0) /\\ ((result = x) \\/ (result = ( -x ) ))) } requires { (x >= -2147483647) }");

        makeTest("Unary minus 5 as parameter", "ensures{\n f (-x) }",
                                               "ensures { ( f ( -x ) ) }");

        makeTest("Formula parametrized with formula", "ensures {"+
                         "p Y (f X Y) }",
             "ensures { ( p Y ( f X Y ) ) }");

        //formula.substitute(new JGTerm("d"), new JGTerm("Y"));
        //System.out.println("After a substitution : "+formula);
    }
}
