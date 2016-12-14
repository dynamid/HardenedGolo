module ascii.converter

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
import java.util
import java.io

let ENCODING = "UTF-8"
var WIDTH_BLOCK = null
var HEIGHT_BLOCK = null
var SYMBOLS = null

----
Input parameters:
  1 - URL input file with source image
  2 - URL file with symbol table
  3 - URL output file
  4 - Width of segmentation block
  5 - Height of segmentation block
----
function main = |args| {
    let image = ImageIO.read(File(args: get(1)))
    HEIGHT_BLOCK = intValue(args: get(5))
    WIDTH_BLOCK = intValue(args: get(4))
    SYMBOLS = fileToText(args: get(2), ENCODING): split("\\s+")

    process(image = image,
            textURL = args: get(3),
            countBlocksX = image: width() / WIDTH_BLOCK,
            countBlocksY = image: height() / HEIGHT_BLOCK)
}

function process = |image, textURL, countBlocksX, countBlocksY| spec/
                                                                    requires {
                                                                        numberBlockX > 0
                                                                     /\ numberBlockY > 0
                                                                    }
                                                                /spec {
    var result = null
    try {
        result = BufferedWriter(OutputStreamWriter(FileOutputStream(textURL), ENCODING))

        foreach y in [0..countBlocksY] {
            foreach x in [0..countBlocksX] {
                let blockColor = computeAverageBlockColor(image, x, y)
                #println(blockColor: Blue() + " " + blockColor: Green() + " " + blockColor: Red() )
                result: write(convertToASCII(blockColor))
            }
            result: write("\r\n")
        }
    } catch (exception) {
        println("Can't open output file: " + exception)
    } finally {
        result?: close()
    }
}

function computeAverageBlockColor = |image, numberBlockX, numberBlockY| spec/
                                                                            requires {
                                                                                numberBlockX > 0
                                                                             /\ numberBlockY > 0
                                                                            }
                                                                        /spec {
    let countPixels = WIDTH_BLOCK * HEIGHT_BLOCK
    let averagePixel = array[0, 0, 0]
    foreach x in [numberBlockX * WIDTH_BLOCK .. (numberBlockX + 1) * WIDTH_BLOCK] {
        foreach y in [numberBlockY * HEIGHT_BLOCK .. (numberBlockY + 1) * HEIGHT_BLOCK] {
            let pixel = Color(image: getRGB(x, y))
            averagePixel: set(0, averagePixel: get(0) + pixel: Red())
            averagePixel: set(1, averagePixel: get(1) + pixel: Green())
            averagePixel: set(2, averagePixel: get(2) + pixel: Blue())
        }
    }
    return Color(averagePixel: get(0) / countPixels,
                 averagePixel: get(1) / countPixels,
                 averagePixel: get(2) / countPixels)
}

function convertToASCII = |color| {
    let redComponent = computeComponentIndex(color: Red())
    let greenComponent = computeComponentIndex(color: Green())
    let blueComponent = computeComponentIndex(color: Blue())
    return SYMBOLS: get(blueComponent * 36 + greenComponent * 6 + redComponent)
}


function computeComponentIndex = |component| spec/
                                                requires {
                                                    component >= 0
                                                 /\ component < 256
                                                }
                                             /spec {
    let index = component / 50
    if (component % 50 > 25) {
        return index + 1
    }
    return index
}
