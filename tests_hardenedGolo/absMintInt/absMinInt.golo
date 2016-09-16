# Run : ./runWithDistri.sh golo --files absMintInt/absMinInt.golo
# Check : ./runWithDistri.sh verify --files absMintInt/absMinInt.golo
#    Println has to be removed to make the check succeed
#    Result : Production of result.mlw
#    Check by : why3 ide result.mlw

# !!!! Dans la version actuelle de l'outil de conversion, le débordement d'Int32n'est pas géré
#   Les actions à faire dans le traducteur pour le gérer :
#     importer int32 au lieu de int
#     constantes numériques littérales de type entier à passer dans la fonction of_int (Ex : of_int 0)
#     constantes numériques littérales de type entier DE LA SPEC à passer dans la fonction to_int (Ex : to_int 0)
#     références de type int en type int32


module test.MinInt

#function myAbs = |x| {
    # spec/ ensures{
    function myAbs = |x| spec/
                                ensures{
                                    (result >= 0) /\
                                    (result = x \/ result = -x)}
                                requires{
                                    x >= -2147483647
                                }

                     /spec {
	if (x < 0){
		return (0 - x)
	} else {
		return x
	}
}

function princ = {
	#let a = myAbs(-2147483648)
    let a = myAbs(-2147483647) + 2
	#let a = myAbs(3)
	return (a)
}

function main = |args| {
    let a = princ()
#    println(a)
}
