package org.eclipse.golo.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.eclipse.golo.cli.command.spi.CliCommand;
import org.eclipse.golo.compiler.GoloCompilationException;
import org.eclipse.golo.compiler.GoloCompiler;
import org.eclipse.golo.compiler.ir.GoloFunction;
import org.eclipse.golo.compiler.ir.GoloModule;
import org.eclipse.golo.compiler.ir.IrTreeVisitAndCheckTypes;
import org.eclipse.golo.compiler.jgoloparser.JGSpecs;
import org.eclipse.golo.compiler.jgoloparser.visitor.JGSpecTreeVisitor;
import org.eclipse.golo.compiler.jgoloparser.visitor.SpecTreeVisitor;
import org.eclipse.golo.compiler.parser.ASTCompilationUnit;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Performs validation types
 */
@Parameters(commandNames = {"verifyTypes"}, commandDescription = "Performs validation of types in the Golo expressions")
public class VerifyTypesCommand implements CliCommand {

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
      verify(new File(file), compiler);
    }
  }

  private void verify(File file, GoloCompiler compiler) {
    if (file.isDirectory()) {
      File[] directoryFiles = file.listFiles();
      if (directoryFiles != null) {
        for (File directoryFile : directoryFiles) {
          verify(directoryFile, compiler);
        }
      }
    } else if (file.getName().endsWith(".golo")) {
      try {
        if (verbose) {
          System.out.println(">>> Verifying file `" + file.getAbsolutePath() + "`");
        }
        compiler.resetExceptionBuilder();
        ASTCompilationUnit compilationUnit = compiler.parse(file.getAbsolutePath());
        GoloModule module = compiler.check(compilationUnit);
        SpecTreeVisitor booleanCheckVisitor = new JGSpecTreeVisitor();
        for (GoloFunction function : module.getFunctions()) {
          booleanCheckVisitor.verify(function);
        }
        new IrTreeVisitAndCheckTypes().visitModule(module);
      } catch (IOException e) {
        System.out.println("[error] " + file + " does not exist or could not be opened.");
      } catch (GoloCompilationException e) {
        handleCompilationException(e, exit);
      }
    }
  }
}
