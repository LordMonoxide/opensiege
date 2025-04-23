package lofimodding.opensiege.formats.skrit;

import lofimodding.opensiege.formats.skrit.exceptions.ExpectedTokenException;
import lofimodding.opensiege.formats.skrit.exceptions.InvalidVariableInitializerException;
import lofimodding.opensiege.formats.skrit.exceptions.SkritCompilerException;
import lofimodding.opensiege.formats.skrit.tokens.SkritAssignmentStatement;
import lofimodding.opensiege.formats.skrit.tokens.SkritFunctionCallStatement;
import lofimodding.opensiege.formats.skrit.tokens.SkritIfStatement;
import lofimodding.opensiege.formats.skrit.tokens.SkritMethod;
import lofimodding.opensiege.formats.skrit.tokens.SkritReturnStatement;
import lofimodding.opensiege.formats.skrit.tokens.SkritStatement;
import lofimodding.opensiege.formats.skrit.tokens.SkritToken;
import lofimodding.opensiege.formats.skrit.tokens.SkritVariable;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritAdd;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritBoolLiteral;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritEquality;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritExpression;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritFloatLiteral;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritFunctionCall;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritIntLiteral;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritMult;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritReadVariable;
import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritStringLiteral;
import lofimodding.opensiege.formats.skrit.types.SkritClassType;
import lofimodding.opensiege.formats.skrit.types.SkritFloatType;
import lofimodding.opensiege.formats.skrit.types.SkritIntType;
import lofimodding.opensiege.formats.skrit.types.SkritType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SkritCompiler {
  public void compile(final InputStream input) {
    final SkritParser parser = new SkritParser(input);

    final Node node;
    try {
      node = parser.CompilationUnit();
    } catch(final ParseException e) {
      throw new SkritCompilerException("Failed to parse skrit", e);
    }

    node.dump("");

    final Compilation compilation = new Compilation(node);
    this.processScript(compilation);
  }

  private void processScript(final Compilation compilation) {
    final List<SkritToken> tokens = new ArrayList<>();

    while(compilation.hasNextChild()) {
      compilation.pushChild();

      tokens.add(switch(compilation.getTokenId()) {
        case SkritParserTreeConstants.JJTPROPERTYDELCARATION, SkritParserTreeConstants.JJTFIELDDECLARATION -> this.processField(compilation);
        case SkritParserTreeConstants.JJTMETHODDECLARATION -> this.processMethod(compilation);
        default -> compilation.unexpectedToken();
      });

      compilation.pop();
    }

    compilation.pop();
  }

  private SkritVariable processField(final Compilation compilation) {
    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTTYPE);

    final SkritType type = this.processType(compilation);

    compilation.pop();
    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTVARIABLEDECLARATOR);

    final SkritVariable variable = this.processVariable(compilation, type, InitializerMode.CONSTANT_INITIALIZER);
    compilation.pop();
    return variable;
  }

  private SkritMethod processMethod(final Compilation compilation) {
    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTMETHODDECLARATOR);
    final String name = compilation.getTokenValue();

    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTFORMALPARAMETERS);
    final List<SkritVariable> params = this.processParameters(compilation);
    compilation.pop();

    compilation.pop();

    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTBLOCK);
    final List<SkritToken> tokens = this.processBlock(compilation);
    compilation.pop();

    return new SkritMethod(name, params, tokens);
  }

  private List<SkritToken> processBlock(final Compilation compilation) {
    final List<SkritToken> tokens = new ArrayList<>();

    while(compilation.hasNextChild()) {
      compilation.pushChild();

      tokens.add(switch(compilation.getTokenId()) {
        case SkritParserTreeConstants.JJTLOCALVARIABLEDECLARATION -> this.processLocalVariable(compilation);
        case SkritParserTreeConstants.JJTSTATEMENT -> this.processStatement(compilation);
        default -> compilation.unexpectedToken();
      });

      compilation.pop();
    }

    return tokens;
  }

  private List<SkritVariable> processParameters(final Compilation compilation) {
    final List<SkritVariable> params = new ArrayList<>();

    while(compilation.hasNextChild()) {
      compilation.pushChild();
      compilation.expectToken(SkritParserTreeConstants.JJTFORMALPARAMETER);
      compilation.pushChild();
      final SkritType type = this.processType(compilation);
      compilation.pop();

      final String name;
      if(compilation.hasNextChild()) {
        name = this.processVariableDeclarator(compilation);
      } else {
        name = "";
      }

      params.add(new SkritVariable(type, name, null));
      compilation.pop();
    }

    return params;
  }

  private SkritStatement processStatement(final Compilation compilation) {
    compilation.pushChild();

    final SkritStatement statement = switch(compilation.getTokenId()) {
      case SkritParserTreeConstants.JJTSTATEMENTEXPRESSION -> {
        compilation.pushChild();

        if(compilation.getTokenId() == SkritParserTreeConstants.JJTFUNCTIONCALL) {
          final SkritStatement function = new SkritFunctionCallStatement(this.processFunctionCall(compilation));
          compilation.pop();
          yield function;
        }

        compilation.backtrack();

        final List<String> names = this.processNames(compilation);

        compilation.pushChild();
        compilation.expectToken(SkritParserTreeConstants.JJTASSIGNMENTOPERATOR);
        compilation.pop();

        final SkritExpression expression = this.processExpression(compilation);
        yield new SkritAssignmentStatement(names, expression);
      }

      case SkritParserTreeConstants.JJTIFSTATEMENT -> {
        final SkritExpression expression = this.processExpression(compilation);

        compilation.pushChild();
        compilation.expectToken(SkritParserTreeConstants.JJTBLOCK);
        final List<SkritToken> block = this.processBlock(compilation);
        compilation.pop();

        yield new SkritIfStatement(expression, block);
      }

      case SkritParserTreeConstants.JJTRETURNSTATEMENT -> new SkritReturnStatement();
      default -> compilation.unexpectedToken();
    };

    compilation.pop();
    return statement;
  }

  private SkritVariable processLocalVariable(final Compilation compilation) {
    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTTYPE);
    final SkritType type = this.processType(compilation);
    compilation.pop();

    compilation.pushChild();
    final SkritVariable variable = this.processVariable(compilation, type, InitializerMode.ANY_INITIALIZER);
    compilation.pop();
    return variable;
  }

  private SkritType processType(final Compilation compilation) {
    compilation.pushChild();

    final SkritType type = switch(compilation.getTokenId()) {
      case SkritParserTreeConstants.JJTPRIMITIVETYPE -> switch(compilation.getTokenValue()) {
        case "int" -> new SkritIntType();
        case "float" -> new SkritFloatType();
        default -> compilation.unexpectedToken();
      };

      case SkritParserTreeConstants.JJTCLASSTYPE -> new SkritClassType(compilation.getTokenValue());
      default -> compilation.unexpectedToken();
    };

    compilation.pop();
    return type;
  }

  private SkritVariable processVariable(final Compilation compilation, final SkritType type, final InitializerMode mode) {
    final String name = this.processVariableDeclarator(compilation);

    final SkritExpression expression;
    if(mode != InitializerMode.NO_INITIALIZER && compilation.hasNextChild()) {
      compilation.pushChild();
      compilation.expectToken(SkritParserTreeConstants.JJTVARIABLEINITIALIZER);

      expression = this.processExpression(compilation);

      if(mode == InitializerMode.CONSTANT_INITIALIZER && !expression.isConstant()) {
        throw new InvalidVariableInitializerException("Variable requires constant initializer");
      }

      compilation.pop();
    } else {
      expression = null;
    }

    return new SkritVariable(type, name, expression);
  }

  private String processVariableDeclarator(final Compilation compilation) {
    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTVARIABLEDECLARATORID);
    final String name = compilation.getTokenValue();
    compilation.pop();
    return name;
  }

  private SkritExpression processExpression(final Compilation compilation) {
    compilation.pushChild();

    final SkritExpression expression = switch(compilation.getTokenId()) {
      case SkritParserTreeConstants.JJTLITERAL -> this.processLiteral(compilation);
      case SkritParserTreeConstants.JJTNAME -> {
        final List<String> names = new ArrayList<>();
        names.add(compilation.getTokenValue());
        yield new SkritReadVariable(this.processNames(compilation, names));
      }
      case SkritParserTreeConstants.JJTFUNCTIONCALL -> this.processFunctionCall(compilation);
      case SkritParserTreeConstants.JJTADD -> new SkritAdd(this.processExpression(compilation), this.processExpression(compilation));
      case SkritParserTreeConstants.JJTMULT -> new SkritMult(this.processExpression(compilation), this.processExpression(compilation));
      case SkritParserTreeConstants.JJTEQUALITY -> new SkritEquality(this.processExpression(compilation), this.processExpression(compilation));
      default -> compilation.unexpectedToken();
    };

    compilation.pop();
    return expression;
  }

  private SkritExpression processLiteral(final Compilation compilation) {
    compilation.pushChild();

    final SkritExpression literal = switch(compilation.getTokenId()) {
      case SkritParserTreeConstants.JJTINTLITERAL -> new SkritIntLiteral(Integer.parseInt(compilation.getTokenValue()));
      case SkritParserTreeConstants.JJTFLOATLITERAL -> new SkritFloatLiteral(Float.parseFloat(compilation.getTokenValue()));
      case SkritParserTreeConstants.JJTBOOLEANLITERAL -> new SkritBoolLiteral(Boolean.parseBoolean(compilation.getTokenValue()));
      case SkritParserTreeConstants.JJTSTRINGLITERAL -> new SkritStringLiteral(compilation.getTokenValue());
      default -> compilation.unexpectedToken();
    };

    compilation.pop();
    return literal;
  }

  private SkritFunctionCall processFunctionCall(final Compilation compilation) {
    final List<String> names = this.processNames(compilation);
    final List<SkritExpression> params = new ArrayList<>();

    compilation.pushChild();
    compilation.expectToken(SkritParserTreeConstants.JJTARGUMENTLIST);

    while(compilation.hasNextChild()) {
      params.add(this.processExpression(compilation));
    }

    compilation.pop();

    return new SkritFunctionCall(names, params);
  }

  private List<String> processNames(final Compilation compilation) {
    return this.processNames(compilation, new ArrayList<>());
  }

  private List<String> processNames(final Compilation compilation, final List<String> names) {
    final int stackStart = names.size();

    while(compilation.hasNextChild()) {
      compilation.pushChild();
      compilation.expectToken(SkritParserTreeConstants.JJTNAME);
      names.add(compilation.getTokenValue());
    }

    if(names.isEmpty()) {
      throw new ExpectedTokenException("Expected names");
    }

    for(int i = stackStart; i < names.size(); i++) {
      compilation.pop();
    }

    return names;
  }

  private enum InitializerMode {
    NO_INITIALIZER,
    CONSTANT_INITIALIZER,
    ANY_INITIALIZER,
  }
}
