package org.eclipse.golo.cli.command;

/**
 * Created by Qifan ZHOU on 05/12/16.
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

@Parameters(commandNames = {"symtest"}, commandDescription = "Generates Symbolic Testing File")
public class SymbolicTestCommand implements CliCommand{

  @Parameter(names = "--files", variableArity = true, description = "Golo source files (*.golo and directories)", required = true)
  List<String> files = new LinkedList<>();

  @Parameter(names = {"--exit"}, description = "Exit on the first encountered error, or continue with the next file")
  boolean exit = false;

  @Parameter(names = {"--verbose"}, description = "Be more verbose")
  boolean verbose = false;


  @Override
  public void execute() throws Throwable {
    GoloCompiler compiler = new GoloCompiler();
    for (String file : files) {
      symtest(new File(file), compiler);
    }
  }

  private void symtest(File file, GoloCompiler compiler) {
    if (file.isDirectory()) {
      File[] directoryFiles = file.listFiles();
      if (directoryFiles != null) {
        for (File directoryFile : directoryFiles) {
          symtest(directoryFile, compiler);
        }
      }
    } else if (file.getName().endsWith(".golo")) {
      try {
        if (verbose) {
          System.out.println(">>> Testing file `" + file.getAbsolutePath() + "`");
        }
        compiler.resetExceptionBuilder();
        compiler.symtest(compiler.parse(file.getAbsolutePath()));
      } catch (IOException e) {
        System.out.println("[error] " + file + " does not exist or could not be opened.");
      } catch (GoloCompilationException e) {
        handleCompilationException(e, exit);
      }
    }
  }
}
