package org.eclipse.golo.cli.command;

/**
 * Created by raphael on 29/04/16.
 */

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.eclipse.golo.cli.command.spi.CliCommand;
import org.eclipse.golo.compiler.GoloCompilationException;
import org.eclipse.golo.compiler.GoloCompiler;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Parameters(commandNames = {"symtest"}, commandDescription = "Generates WhyML program from Golo source file")
public class SymbolicTestCommand implements CliCommand{

  @Parameter(names = "--files", variableArity = true, description = "Golo source files (*.golo and directories)", required = true)
  List<String> files = new LinkedList<>();

  @Parameter(names = {"-o"}, description = "WHYML output file. If one already exists, it will be overwritten")
  String destFile;

  @Parameter(names = {"--exit"}, description = "Exit on the first encountered error, or continue with the next file")
  boolean exit = false;

  @Parameter(names = {"--verbose"}, description = "Be more verbose")
  boolean verbose = false;

  @Parameter(names = {"--int32"}, description = "Consider bounded integers on 32bits")
  boolean int32 = false;


  @Override
  public void execute() throws Throwable {
    GoloCompiler compiler = new GoloCompiler();
    for (String file : files) {
      verify(new File(file), compiler, destFile);
    }
  }

  private void verify(File file, GoloCompiler compiler, String destFile) {
    if (file.isDirectory()) {
      File[] directoryFiles = file.listFiles();
      if (directoryFiles != null) {
        for (File directoryFile : directoryFiles) {
          verify(directoryFile, compiler, destFile);
        }
      }
    } else if (file.getName().endsWith(".golo")) {
      try {
        if (verbose) {
          System.out.println(">>> Verifying file `" + file.getAbsolutePath() + "`");
        }
        compiler.resetExceptionBuilder();
        compiler.verify(compiler.parse(file.getAbsolutePath()), file.getAbsolutePath(), destFile, int32);
      } catch (IOException e) {
        System.out.println("[error] " + file + " does not exist or could not be opened.");
      } catch (GoloCompilationException e) {
        handleCompilationException(e, exit);
      }
    }
  }
}
