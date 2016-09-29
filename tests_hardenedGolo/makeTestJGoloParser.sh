echo ""
echo "==========="
echo "Reminder : more details by running"
echo "      ./makeTestJGoloParser.sh --verbose"
echo "==========="
echo ""

echo "Making model"
javac ../src/main/java/org/eclipse/golo/compiler/jgoloparser/*.java -d testsJGoloParser

# On n'exécute la suite que s'il n'y a pas d'erreur dans celle-ci ($? est le code de retour de la dernière commande exécutée)
if [ "$?" -eq "0" ]
then
    echo "Making parser"
    java -cp javacc-6.1.2.jar javacc -OUTPUT_DIRECTORY=testsJGoloParser/org/eclipse/golo/compiler/jgoloparser/. ../src/main/jjtree/org/eclipse/golo/compiler/jgoloparser/FolJGolo.jjt
    if [ "$?" -eq "0" ]
    then
        javac -cp testsJGoloParser testsJGoloParser/org/eclipse/golo/compiler/jgoloparser/*.java -d testsJGoloParser

        if [ "$?" -eq "0" ]
        then
            cd testsJGoloParser
            javac testJGoloParser.java
            if [ "$?" -eq "0" ]
            then
                java testJGoloParser $*
            fi
        fi
    fi
fi
