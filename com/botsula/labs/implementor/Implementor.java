package com.botsula.labs.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


/**
 * Generate an implementation of passed class and writes it to the specified directory
 * @author Botsula Konstantin
 */
public class Implementor implements JarImpler {
    /**
     * Suffix of the generated class implementation
     */
    private static final String IMPL_SUFFIX = "Impl";

    /**
     * Extension for java constant
     */
    private static final String JAVA = ".java";

    /**
     * Extension for class constant
     */
    private static final String CLASS = ".class";

    /**
     * File separator constant
     */
    private static final char FILE_SEPARATOR = File.separatorChar;

    /**
     * New line separator constant
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Constant for tabulation
     */
    private static final String TAB = "    ";

    /**
     * End of the code line constant
     */
    private static final String EOL = ";" + LINE_SEPARATOR;

    /**
     * Constant for space
     */
    private static final String SPACE = " ";

    /**
     * Open brace for begin of code block constant
     */
    private static final String BEGIN = "{" + LINE_SEPARATOR;

    /**
     * Close brace for end of code block constant
     */
    private static final String END = "}" + LINE_SEPARATOR;

    /**
     * Creates instance of {@link Implementor}
     */
    public Implementor() {}


    /**
     * Additional class for {@link Method methods} to wrap them.
     */
    private class MethodBox {
        /**
         * {@link Method}
         */
        private Method method;
        /**
         * Constructor
         *
         * @param method underlying {@link Method}
         */
        MethodBox(Method method) {
            this.method = method;
        }

        /**
         * Get's name of wrapped <code>method</code>
         *
         * @return name of the  <code>method</code>
         */
        private String getName() {
            return method.getName();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return getName().hashCode() * 31
                    + Arrays.hashCode(method.getParameterTypes());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodBox)) {
                return false;
            }
            MethodBox other = (MethodBox) obj;
            return Objects.equals(getName(), other.getName())
                    && Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes());
        }

    }

    /**
     * Resolves path file.
     *
     * @param path path file with code
     * @param token  type-token
     * @param extension string representation of extension.
     * @return path to file
     */
    private Path resolveFilePath(Path path, Class<?> token, String extension) {
        return path.resolve(token.getPackage().getName().replace('.', FILE_SEPARATOR)).resolve(token.getSimpleName() + IMPL_SUFFIX + extension);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Required non-null arguments");
        }
        if (token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers()) || token == Enum.class) {
            throw new ImplerException("Incorrect type");
        }


        Path path = resolveFilePath(root, token, JAVA);

        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Couldn't create output file: " + e.getMessage());
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            generateHead(token, writer);
            generateConstructors(token, writer);
            generateMethods(token, writer);
            writer.write(toUnicode(END));
        } catch (IOException e) {
            throw new ImplerException("couldn't write to output file");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path tmpDir = Paths.get(".");
        implement(token, tmpDir);
        Path filepath = resolveFilePath(tmpDir, token, JAVA);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compiler not found");
        }
        String[] args = {"-encoding", "cp866", "-cp", tmpDir.toString() + File.pathSeparator + getClassPath(token), filepath.toString()};
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("couldn't compile class");
        }

        Path compiledClassPath = resolveFilePath(tmpDir, token, CLASS);
        compiledClassPath.toFile().deleteOnExit();
        String classname = token.getName().replace(".", "/") + IMPL_SUFFIX + CLASS;
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Tigran Vanyan");

        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            writer.putNextEntry(new ZipEntry(classname));
            Files.copy(compiledClassPath, writer);
        } catch (IOException e) {
            throw new ImplerException("couldn't write to jar file");
        }
    }

    /**
     * Convert array representation of methods to a set form.
     *
     * @param methods array of methods.
     * @param storage set, which collected abstract methods from methods
     */
    private void addToMethodStorage(Method[] methods, Set<MethodBox> storage) {
        Arrays.stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodBox::new)
                .collect(Collectors.toCollection(() -> storage));
    }

    /**
     * Generates methods declarations and default bodies of a given <code>token</code> and prints them
     *
     * @param token token, which methods are to be generated
     * @param writer output facility
     * @throws IOException if couldn't write a generated code
     */
    private void generateMethods(Class<?> token, BufferedWriter writer) throws IOException {
        StringBuilder result = new StringBuilder();
        Set<MethodBox> methods = new HashSet<>();
        addToMethodStorage(token.getMethods(), methods);

        if (!token.isInterface()) {
            for (Class<?> t = token; t != null; t = t.getSuperclass()) {
                addToMethodStorage(t.getDeclaredMethods(), methods);
            }
        }

        for (MethodBox method : methods) {
            result.append(getExecutable(method.method));
            result.append(LINE_SEPARATOR);
        }

        writer.write(toUnicode(result.toString()));
    }

    /**
     * To Unicode representation
     *
     * @param target String which is to be Unicoded
     * @return Unicoded string
     */
    private String toUnicode(String target) {
        StringBuilder escapeBuilder = new StringBuilder();
        for (char c : target.toCharArray()) {
            if (c >= 128)
                escapeBuilder.append("\\u").append(String.format("%04X", (int) c));
            else
                escapeBuilder.append(c);
        }
        return escapeBuilder.toString();
    }

    /**
     * Returns a classpath of a given <code>token</code>
     *
     * @param token token to get classpath
     * @return {@link String} corresponding to a classpath of a given <code>token</code>
     */
    private String getClassPath(Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Generates code, which contains package name of a class and its declaration, and prints them
     *
     * @param token class to be extended
     * @param writer output
     * @throws IOException if couldn't write a generated code
     */
    private void generateHead(Class<?> token, BufferedWriter writer) throws IOException {
        String packageName = token.getPackageName();
        if (packageName != null) {
            writer.write(toUnicode("package" + SPACE + packageName + EOL ));
        }

        String declaration = "public class" + SPACE + token.getSimpleName() + IMPL_SUFFIX + SPACE
                + (token.isInterface() ? "implements" : "extends") + SPACE + token.getSimpleName() + SPACE + BEGIN;

        writer.write(toUnicode(declaration));
    }

    /**
     * Generates declarations and default body of non-private constructors of a given <code>token</code> and prints them
     *
     * @param token class, which constructed are to be generated
     * @param writer output
     * @throws IOException if couldn't write a generated code
     * @throws ImplerException if a given class has no public constructors
     */
    private void generateConstructors(Class<?> token, BufferedWriter writer) throws IOException, ImplerException {
        if (token.isInterface()) {
            return;
        }
        List<Constructor> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .collect(Collectors.toList());

        if (constructors.isEmpty()) {
            throw new ImplerException("Class do not have public constructors");
        }
        for (Constructor constructor : constructors) {
            writer.write(toUnicode(getExecutable(constructor)));
        }
    }

    /**
     * Generates declaration and default body of given <code>executable</code>
     *
     * @param executable {@link Executable} to be generated.
     * @return {@link String} for <code>executable</code> full code
     */
    private String getExecutable(Executable executable) {
        String declaration = getDeclaration(executable);
        String body = getBody(executable);

        return TAB + declaration + SPACE + BEGIN + body + TAB + END;
    }

    /**
     * Generates declaration of given <code>executable</code>
     *
     * @param executable {@link Executable} to be generated.
     * @return {@link String} for the declaration
     */
    private String getDeclaration(Executable executable) {
        StringBuilder declaration = new StringBuilder();
        declaration.append(getModifiers(executable))
                .append(SPACE);

        if (executable instanceof Constructor) {
            declaration.append(executable.getDeclaringClass().getSimpleName());
            declaration.append(IMPL_SUFFIX);
        } else {
            declaration.append(getReturnType(executable))
                    .append(SPACE)
                    .append(executable.getName());
        }

        declaration.append("(")
                .append(getParameters(executable, true))
                .append(")");

        String exceptions = getExceptions(executable);
        if (!exceptions.isEmpty()) {
            declaration.append(SPACE)
                    .append("throws")
                    .append(SPACE)
                    .append(exceptions);
        }

        return declaration.toString();
    }

    /**
     * Generates default body of given <code>executable</code>
     *
     * @param executable {@link Executable} to be generated.
     * @return {@link String} for the body
     */
    private String getBody(Executable executable) {
        String res = TAB + TAB;
        if (executable instanceof Constructor) {
            res += "super(" + getParameters(executable, false) + ")";
        } else {
            res += "return" + SPACE + getDefaultValue(((Method) executable).getReturnType());
        }
        return res + EOL;
    }

    /**
     * Generates parameters of a given <code>executable</code>
     *
     * @param executable {@link Executable}, which parameters to be extracted.
     * @param types of parameters are required or not
     * @return {@link String}, for parameters names
     */
    private String getParameters(Executable executable, boolean types) {
        List<String> res = new ArrayList<>();
        Parameter[] params = executable.getParameters();
        for (Parameter param : params) {
            res.add((types ? param.getType().getCanonicalName() + SPACE : "") + param.getName());
        }

        return res.stream().reduce((String a, String b) -> (a + ", " + b)).orElse("");

    }

    /**
     * Generates exceptions, which a given <code>executable</code> might throw
     *
     * @param executable {@link Executable}, which exceptions to be extracted
     * @return {@link String}, for exceptions
     */
    private String getExceptions(Executable executable) {
        return Arrays.stream(executable.getExceptionTypes())
                .map(Class::getName).reduce((String a, String b) -> (a + ", " + b)).orElse("");
    }

    /**
     * Returns default value of a given <code>token</code>
     *
     * @param token default value of that type-token
     * @return {@link String}, for default value
     */
    private String getDefaultValue(Class<?> token) {
        if (token.getSimpleName().equals("void")) {
            return "";
        }
        if (token.getSimpleName().equals("boolean")) {
            return "false";
        }
        if (token.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    /**
     * Returns modifiers of a given <code>executable</code>
     *
     * @param executable {@link Executable}, which modifiers to be extracted
     * @return {@link String}, for modifiers
     */
    private String getModifiers(Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }

    /**
     * Returns return type of a given <code>executable</code>
     *
     * @param executable {@link Executable}, which return type to be extracted
     * @return {@link String}, for the return type
     */
    private String getReturnType(Executable executable) {
        if (executable instanceof Constructor) {
            return "";
        } else {
            return ((Method)executable).getReturnType().getCanonicalName();
        }
    }

    /**
     * Main function
     *
     * @param args arguments for execution.
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Two or three arguments were expected");
            return;
        }

        for (String arg : args) {
            if (arg == null) {
                System.out.println("Non-null arguments were expected");
                return;
            }
        }

        JarImpler implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                System.out.println(args[0] + " is unknown argument, -jar expected.");
            }
        } catch (InvalidPathException e) {
            System.out.println("Invalid path in the second argument. " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Invalid class in the first argument. " + e.getMessage());

        } catch (ImplerException e) {
            System.out.println("An error occurred during implementation. " + e.getMessage());
        }
    }
}
