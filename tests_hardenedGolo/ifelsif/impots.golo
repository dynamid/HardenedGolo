module ifelsif.impots
# Run : ./runWithDistri.sh golo --files ifelsif/impots.golo
# Check : ./runWithDistri.sh verify --files ifelsif/impots.golo
#    Println has to be removed to make the check succeed
#    Result : Production of result.mlw
#    Check by : why3 ide result.mlw

# function myAbs = |x| spec/ ensures{
#function myAbs = |x| spec/ requires{
#                               to_int x >= -2147483647
#                           }
#                           ensures{
#                               (to_int result >= 0) /\
#                               (to_int result = to_int x \/ to_int result = -(to_int x))}
#                     /spec {




function calculImpot = |n, r| {
    # Ni N, ni R ne peut être nul ou négatif.
    # N : nombre de parts
    # R : revenus
    let ratio = r/n
    var impot = 0

    if(ratio <=20000) {
        impot = r/16 - 25*n

    } else if(ratio <=30000) {
        impot = r/8 - 50*n

    } else if(ratio <=40000) {
        impot = r/4 - 100*n

    } else {
        impot = r/2 - 200*n
    }


	return (impot)
}

# TODO : gestion des fonctions de la bilbiothèque (println en particulier)
function main = |args| {
    let a = calculImpot(1, 40000)
    # println(a)
}
