/*
 * Copyright (c) 2009.  The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
grammar HBql;

options {superClass=ParserSupport;}

tokens {
	SEMI = ';';
	DOT = '.';
	DOLLAR = '$';
	COLON = ':';
	QMARK = '?';
	STAR = '*';
	DIV = '/';
	COMMA = ',';
	PLUS = '+';
	MINUS = '-';
	MOD = '%';
	EQ = '=';
	LT = '<';
	GT = '>';
	LTGT = '<>';	
	LTEQ = '<=';	
	GTEQ = '>=';	
	BANGEQ = '!=';	
	DQUOTE = '"';
	SQUOTE = '\'';
	LPAREN = '(';
	RPAREN = ')';
	LBRACE = '[';
	RBRACE = ']';
	LCURLY = '{';
	RCURLY = '}';
}

@lexer::members {
  public void reportError(RecognitionException e) {
    throw new LexerRecognitionException(e, e.getMessage());
  }
}
@rulecatch {catch (RecognitionException re) {handleRecognitionException(re);}}

@header {
package org.apache.hadoop.hbase.hbql.antlr;

import org.apache.hadoop.hbase.hbql.parser.*;
import org.apache.hadoop.hbase.hbql.statement.*;
import org.apache.hadoop.hbase.hbql.statement.args.*;
import org.apache.hadoop.hbase.hbql.statement.select.*;
import org.apache.hadoop.hbase.hbql.schema.*;
import org.apache.hadoop.hbase.hbql.schema.property.*;

import org.apache.expreval.expr.*;
import org.apache.expreval.expr.node.*;
import org.apache.expreval.expr.betweenstmt.*;
import org.apache.expreval.expr.calculation.*;
import org.apache.expreval.expr.casestmt.*;
import org.apache.expreval.expr.compare.*;
import org.apache.expreval.expr.function.*;
import org.apache.expreval.expr.ifthenstmt.*;
import org.apache.expreval.expr.instmt.*;
import org.apache.expreval.expr.literal.*;
import org.apache.expreval.expr.nullcomp.*;
import org.apache.expreval.expr.stringpattern.*;
import org.apache.expreval.expr.var.*;
import org.apache.expreval.util.*;
}

@lexer::header {
package org.apache.hadoop.hbase.hbql.antlr;
import org.apache.expreval.client.*;
import org.apache.hadoop.hbase.hbql.parser.*;
}

consoleStatements returns [List<HBqlStatement> retval]
@init {retval = Lists.newArrayList();}
	: c1=consoleStatement SEMI {retval.add($c1.retval);} ((c2=consoleStatement {retval.add($c2.retval);})? SEMI )*
	;
	
consoleStatement returns [HBqlStatement retval]
options {backtrack=true;}	
	: keySHOW keyTABLES 		 		{retval = new ShowTablesStatement();}
	| keySHOW keySCHEMAS 		 		{retval = new ShowSchemasStatement();}
	| keyIMPORT val=QSTRING				{retval = new ImportStatement($val.text);}
	| keyPARSE c=consoleStatement			{retval = new ParseStatement($c.retval);}
	| keyPARSE keyEXPR te=topExpr			{retval = new ParseStatement($te.retval);}
	| keySET t=simpleName EQ? val=QSTRING	 	{retval = new SetStatement($t.text, $val.text);}
	| keyVERSION					{retval = new VersionStatement();}
	| keyHELP					{retval = new HelpStatement();}
	| jdbc=jdbcStatement				{retval = $jdbc.retval;}
	;						

jdbcStatement returns [HBqlStatement retval]
options {backtrack=true;}	
	: s1=schemaStatement				{retval = $s1.retval;}
	| s2=schemaManagerStatement			{retval = $s2.retval;}
	| s3=tableStatement				{retval = $s3.retval;}
	;
	
schemaStatement returns [SchemaContext retval]
	: keyDELETE keyFROM keySCHEMA? t=simpleName w=withClause?	
							{retval = new DeleteStatement($t.text, $w.retval);}
	| keyINSERT keyINTO keySCHEMA? t=simpleName LPAREN e=exprList RPAREN ins=insertValues
							{retval = new InsertStatement($t.text, $e.retval, $ins.retval);}
	| sel=selectStatement				{retval = $sel.retval;}			
	;

insertValues returns [InsertValueSource retval]
	: keyVALUES LPAREN e=insertExprList RPAREN	{retval = new InsertSingleRow($e.retval);}
	| sel=selectStatement				{retval = new InsertSelectValues($sel.retval);}			
	;
			
selectStatement returns [SelectStatement retval]
	: keySELECT c=selectElems keyFROM keySCHEMA? t=simpleName w=withClause?
							{retval = new SelectStatement($c.retval, $t.text, $w.retval);};
							
schemaManagerStatement returns [SchemaContext retval]
	: keyCREATE (tmp=keyTEMP)? keySCHEMA t=simpleName (keyFOR keyTABLE a=simpleName)? LPAREN l=attribList RPAREN
							{retval = new CreateSchemaStatement(($tmp.text != null),$t.text, $a.text, $l.retval);}
	| keyDROP keySCHEMA t=simpleName 		{retval = new DropSchemaStatement($t.text);}
	| keyDESCRIBE keySCHEMA t=simpleName 		{retval = new DescribeSchemaStatement($t.text);}
	;

tableStatement returns [TableStatement retval]
	: keyCREATE keyTABLE t=simpleName LPAREN f=familyDefinitionList RPAREN	
							{retval = new CreateTableStatement($t.text, $f.retval);}
	| keyDESCRIBE keyTABLE t=simpleName 		{retval = new DescribeTableStatement($t.text);}
	| keyDISABLE keyTABLE t=simpleName 		{retval = new DisableTableStatement($t.text);}
	| keyDROP keyTABLE t=simpleName 		{retval = new DropTableStatement($t.text);}
	| keyENABLE keyTABLE t=simpleName 		{retval = new EnableTableStatement($t.text);}
	;

familyDefinitionList returns [List<FamilyDefinition> retval]
@init {retval = Lists.newArrayList();}
	: (a1=familyDefinition {retval.add($a1.retval);} (COMMA a2=familyDefinition {retval.add($a2.retval);})*)?;

familyDefinition returns [FamilyDefinition retval]
	: f=simpleName LPAREN p=familyPropertyList RPAREN	
							{retval = new FamilyProperty($f.text, $p.retval);};

familyPropertyList returns [List<FamilyProperty> retval]							
@init {retval = Lists.newArrayList();}
	: (a1=familyProperty {retval.add($a1.retval);} (COMMA a2=familyProperty {retval.add($a2.retval);})*)?;
	
familyProperty returns [FamilyProperty retval]
	: keyMAX keyVERSIONS v=valPrimary		{retval = new VersionProperty($v.retval);}
	| keyBLOOM keyFILTER b=topExpr			{retval = new BloomFilterProperty($b.retval);}
	| keyBLOCK keySIZE v=valPrimary			{retval = new BlockSizeProperty($b.retval);}
	| keyBLOCK keyCACHE v=valPrimary		{retval = new BlockCacheProperty($b.retval);}
	| keyCOMPRESSION keyTYPE v=valPrimary		{retval = new CompressionTypeProperty($b.retval);}
	| keyIN keyMEMORY v=valPrimary			{retval = new InMemoryProperty($b.retval);}
	| keyMAP keyFILE keyINDEX keyINTERVAL v=valPrimary		
							{retval = new MapFileIndexIntervalProperty($b.retval);}
	| keyTTL v=valPrimary				{retval = new TtlProperty($b.retval);}
	;
			 
withClause returns [WithArgs retval]
@init {retval = new WithArgs();}
	: keyWITH withElements[retval]+;

withElements[WithArgs withArgs] 
	: k=keysRangeArgs				{withArgs.setKeyRangeArgs($k.retval);}
	| t=timestampArgs				{withArgs.setTimestampArgs($t.retval);}	
	| v=versionArgs					{withArgs.setVersionArgs($v.retval);}
	| l=limitArgs					{withArgs.setLimitArgs($l.retval);}
	| s=serverFilter				{withArgs.setServerExpressionTree($s.retval);}
	| c=clientFilter				{withArgs.setClientExpressionTree($c.retval);}
	;
	
keysRangeArgs returns [KeyRangeArgs retval]
	: keyKEYS k=rangeList				{retval = new KeyRangeArgs($k.retval);}	
	| keyKEYS keyALL				{retval = new KeyRangeArgs();}	
	;

rangeList returns [List<KeyRangeArgs.Range> retval]
@init {retval = Lists.newArrayList();}
	: k1=keyRange {retval.add($k1.retval);} (COMMA k2=keyRange {retval.add($k2.retval);})*;
	
keyRange returns [KeyRangeArgs.Range retval]
options {backtrack=true;}	
	: q1=valPrimary keyTO keyLAST			{retval = KeyRangeArgs.newLastRange($q1.retval);}
	| keyFIRST keyTO q1=valPrimary			{retval = KeyRangeArgs.newFirstRange($q1.retval);}
	| q1=valPrimary keyTO q2=valPrimary		{retval = KeyRangeArgs.newRange($q1.retval, $q2.retval);}
	| q1=valPrimary 				{retval = KeyRangeArgs.newSingleKey($q1.retval);}
	;
		
timestampArgs returns [TimestampArgs retval]
	: keyTIMESTAMP keyRANGE d1=valPrimary keyTO d2=valPrimary	
							{retval = new TimestampArgs($d1.retval, $d2.retval);}
	| keyTIMESTAMP d1=valPrimary			{retval = new TimestampArgs($d1.retval);}
	;
		
versionArgs returns [VersionArgs retval]
	: keyVERSIONS v=valPrimary			{retval = new VersionArgs($v.retval);}
	| keyVERSIONS keyMAX				{retval = new VersionArgs(new IntegerLiteral(Integer.MAX_VALUE));}
	;
	
limitArgs returns [LimitArgs retval]
	: keyLIMIT v=valPrimary				{retval = new LimitArgs($v.retval);};
		
clientFilter returns [ExpressionTree retval]
	: keyCLIENT keyFILTER keyWHERE w=descWhereExpr	
							{retval = $w.retval;};
	
serverFilter returns [ExpressionTree retval]
	: keySERVER keyFILTER keyWHERE w=descWhereExpr	
							{retval = $w.retval;};
	
nodescWhereExpr returns [ExpressionTree retval]
	 : e=topExpr					{retval = ExpressionTree.newExpressionTree(null, $e.retval);};

descWhereExpr returns [ExpressionTree retval]
	: s=schemaDesc? e=topExpr			{retval = ExpressionTree.newExpressionTree($s.retval, $e.retval);};

// Expressions
topExpr returns [GenericValue retval]
	: o=orExpr					{retval = $o.retval;};
				
orExpr returns [GenericValue retval]
@init {List<GenericValue> exprList = Lists.newArrayList(); List<Operator> opList = Lists.newArrayList(); }
	: e1=andExpr {exprList.add($e1.retval);} (keyOR e2=andExpr {opList.add(Operator.OR); exprList.add($e2.retval);})* 
							{retval = getLeftAssociativeBooleanCompare(exprList, opList);};

andExpr returns [GenericValue retval]
@init {List<GenericValue> exprList = Lists.newArrayList(); List<Operator> opList = Lists.newArrayList(); }
	: e1=notExpr {exprList.add($e1.retval);} (keyAND e2=notExpr {opList.add(Operator.AND); exprList.add($e2.retval);})* 
							{retval = getLeftAssociativeBooleanCompare(exprList, opList);};

notExpr returns [GenericValue retval]			 
	: (n=keyNOT)? p=eqneExpr			{retval = ($n.text != null) ? new BooleanNot(true, $p.retval) :  $p.retval;};

eqneExpr returns [GenericValue retval]
options {backtrack=true; memoize=true;}	
	: v1=ltgtExpr o=eqneOp v2=ltgtExpr 		{retval = new DelegateCompare($v1.retval, $o.retval, $v2.retval);}	
	| c=ltgtExpr					{retval = $c.retval;}
	;

ltgtExpr returns [GenericValue retval]
options {backtrack=true; memoize=true;}	
	: v1=valPrimary o=ltgtOp v2=valPrimary		{retval = new DelegateCompare($v1.retval, $o.retval, $v2.retval);}
	| b=booleanFunctions				{retval = $b.retval;}
	| p=valPrimary					{retval = $p.retval;}
	;

// Value Expressions
valPrimary returns [GenericValue retval] 
@init {List<GenericValue> exprList = Lists.newArrayList(); List<Operator> opList = Lists.newArrayList(); }
	: e1=multExpr {exprList.add($e1.retval);} (op=plusMinus e2=multExpr {opList.add($op.retval); exprList.add($e2.retval);})*	
							{retval = getLeftAssociativeCalculation(exprList, opList);};
	
multExpr returns [GenericValue retval]
@init {List<GenericValue> exprList = Lists.newArrayList(); List<Operator> opList = Lists.newArrayList(); }
	: e1=signedExpr {exprList.add($e1.retval);} (op=multDiv e2=signedExpr {opList.add($op.retval); exprList.add($e2.retval);})*	
							{retval = getLeftAssociativeCalculation(exprList, opList);};
	
signedExpr returns [GenericValue retval]
	: (s=plusMinus)? n=parenExpr 			{retval = ($s.retval == Operator.MINUS) ? new DelegateCalculation($n.retval, Operator.NEGATIVE, new IntegerLiteral(0)) : $n.retval;};

// The order here is important.  atomExpr has to come after valueFunctions to avoid simpleName conflict
parenExpr returns [GenericValue retval]
options {backtrack=true; memoize=true;}	
	: f=valueFunctions				{retval = $f.retval;}
	| n=atomExpr					{retval = $n.retval;}
	| LPAREN s=topExpr RPAREN			{retval = $s.retval;}
	;
	   						 
atomExpr returns [GenericValue retval]
	: s=stringLiteral				{retval = $s.retval;}
	| i=integerLiteral				{retval = $i.retval;}
	| l=longLiteral					{retval = $l.retval;}
	| d=doubleLiteral				{retval = $d.retval;}
	| b=booleanLiteral				{retval = $b.retval;}
	| keyNULL					{retval = new StringNullLiteral();}
	| p=paramRef					{retval = new NamedParameter($p.text);}
	| v=varRef					{retval = new DelegateColumn($v.text);}
	;

// Literals		
stringLiteral returns [StringLiteral retval]
	: v=QSTRING 					{retval = new StringLiteral($v.text);};
	
integerLiteral returns [IntegerLiteral retval]
	: v=INT						{retval = new IntegerLiteral($v.text);};	

longLiteral returns [LongLiteral retval]
	: v=LONG					{retval = new LongLiteral($v.text);};	


doubleLiteral returns [GenericValue retval]
	: v1=DOUBLE1					{retval = DoubleLiteral.valueOf($v1.text);}
	| v2=DOUBLE2					{retval = DoubleLiteral.valueOf($v2.text);}
	;	

booleanLiteral returns [BooleanLiteral retval]
	: t=keyTRUE					{retval = new BooleanLiteral($t.text);}
	| f=keyFALSE					{retval = new BooleanLiteral($f.text);}
	;

// Functions
booleanFunctions returns [BooleanValue retval]
options {backtrack=true; memoize=true;}	
	: s1=valPrimary n=keyNOT? keyCONTAINS s2=valPrimary		
							{retval = new ContainsStmt($s1.retval, ($n.text != null), $s2.retval);}
	| s1=valPrimary n=keyNOT? keyLIKE s2=valPrimary {retval = new LikeStmt($s1.retval, ($n.text != null), $s2.retval);}
	| s1=valPrimary n=keyNOT? keyBETWEEN s2=valPrimary keyAND s3=valPrimary		
							{retval = new DelegateBetweenStmt($s1.retval, ($n.text != null), $s2.retval, $s3.retval);}
	| s1=valPrimary n=keyNOT? keyIN LPAREN l=exprList RPAREN			
							{retval = new DelegateInStmt($s1.retval, ($n.text != null), $l.retval);} 
	| s1=valPrimary keyIS n=keyNOT? keyNULL		{retval = new DelegateNullCompare(($n.text != null), $s1.retval);}	
	;

valueFunctions returns [GenericValue retval]
options {backtrack=true; memoize=true;}	
	: keyIF t1=topExpr keyTHEN t2=topExpr keyELSE t3=topExpr keyEND	
							{retval = new DelegateIfThen($t1.retval, $t2.retval, $t3.retval);}
	
	| c=caseStmt					{retval = $c.retval;} 						

	| t=simpleName LPAREN a=exprList? RPAREN	{retval = new DelegateFunction($t.text, $a.retval);}
	;

caseStmt returns [DelegateCase retval]
	: keyCASE 					{retval = new DelegateCase();}
	   whenItem[retval]+
	   (keyELSE t=topExpr)? 			{retval.addElse($t.retval);}
	  keyEND
	;
	
whenItem [DelegateCase stmt] 
	: keyWHEN t1=topExpr keyTHEN t2=topExpr		{stmt.addWhen($t1.retval, $t2.retval);}
	;
	
attribList returns [List<ColumnDescription> retval] 
@init {retval = Lists.newArrayList();}
	: (a1=attribDesc {retval.add($a1.retval);} (COMMA a2=attribDesc {retval.add($a2.retval);})*)?;
	
attribDesc returns [ColumnDescription retval]
	: c=varRef type=simpleName (b=LBRACE RBRACE)? (keyALIAS a=simpleName)? (keyDEFAULT t=topExpr)?	
							{retval = ColumnDescription.newColumn($c.text, $a.text, false, $type.text, $b.text!=null, $t.retval);}
	| f=familyWildCard (keyALIAS a=simpleName)?	{retval = ColumnDescription.newFamilyDefault($f.text, $a.text);}
	;

selectElems returns [List<SelectElement> retval]
	: STAR						{retval = FamilySelectElement.newAllFamilies();}
	| c=selectElemList				{retval = $c.retval;}
	;
	
selectElemList returns [List<SelectElement> retval]
@init {retval = Lists.newArrayList();}
	: c1=selectElem {retval.add($c1.retval);} (COMMA c2=selectElem {retval.add($c2.retval);})*;

selectElem returns [SelectElement retval]
options {backtrack=true; memoize=true;}	
	: b=topExpr (keyAS i2=simpleName)?		{retval = SingleExpressionContext.newSingleExpression($b.retval, $i2.text);}
	| f=familyWildCard					{retval = FamilySelectElement.newFamilyElement($f.text);}
	;

exprList returns [List<GenericValue> retval]
@init {retval = Lists.newArrayList();}
	: i1=topExpr {retval.add($i1.retval);} (COMMA i2=topExpr {retval.add($i2.retval);})*;
				
insertExprList returns [List<GenericValue> retval]
@init {retval = Lists.newArrayList();}
	: i1=insertExpr {retval.add($i1.retval);} (COMMA i2=insertExpr {retval.add($i2.retval);})*;

insertExpr returns [GenericValue retval]
	: t=topExpr					{retval = $t.retval;} 
	| keyDEFAULT					{retval = new DefaultKeyword();}
	;
					
schemaDesc returns [Schema retval]
	: LCURLY a=attribList RCURLY			{retval = newHBaseSchema(input, $a.retval);};
	
ltgtOp returns [Operator retval]
	: GT 						{retval = Operator.GT;}
	| GTEQ 						{retval = Operator.GTEQ;}
	| LT 						{retval = Operator.LT;}
	| LTEQ 						{retval = Operator.LTEQ;}
	;
			
eqneOp returns [Operator retval]
	: EQ EQ?					{retval = Operator.EQ;}
	| (LTGT | BANGEQ)				{retval = Operator.NOTEQ;}
	;
				
qstring	: QSTRING ;					

plusMinus returns [Operator retval]
	: PLUS						{retval = Operator.PLUS;}
	| MINUS						{retval = Operator.MINUS;}
	;
	
multDiv returns [Operator retval]
	: STAR						{retval = Operator.MULT;}
	| DIV						{retval = Operator.DIV;}
	| MOD						{retval = Operator.MOD;}
	;

simpleName
 	: ID;
 	
varRef 
	: ID (COLON ID)?;
	
familyWildCard 
	: ID COLON STAR;
	
paramRef
	: COLON ID
	| QMARK;
		
INT	: DIGIT+;
LONG	: DIGIT+ ('L' | 'l');
DOUBLE1	: DIGIT+ (DOT DIGIT*)? ('D' | 'd' | 'F' | 'f');
DOUBLE2	: DIGIT+ DOT DIGIT*;

ID : CHAR (CHAR | DOT | MINUS | DOLLAR | DIGIT)*; // DOLLAR is for inner class table names

fragment
DIGIT	: '0'..'9'; 

fragment
CHAR 	: 'a'..'z' | 'A'..'Z' | '_'; 
	 
QSTRING		
@init {final StringBuilder sbuf = new StringBuilder();}	
	: DQUOTE (options {greedy=false;} : any=DQCHAR {sbuf.append(ParserSupport.decodeEscapedChar($any.getText()));})* DQUOTE {setText(sbuf.toString());}
	| SQUOTE (options {greedy=false;} : any=SQCHAR {sbuf.append(ParserSupport.decodeEscapedChar($any.getText()));})* SQUOTE {setText(sbuf.toString());}
	;

fragment 
DQCHAR
    : ESC_SEQ | ~('\\'|'\"');
    	
fragment 
SQCHAR
    : ESC_SEQ | ~('\\'|'\'');
    	
fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

COMMENT
    :   '//' ~('\n'|'\r')* '\r'? '\n' {skip();}
    |   '/*' ( options {greedy=false;} : . )* '*/' {skip();}
    ;

WS 	: (' ' |'\t' |'\n' |'\r' )+ {skip();};

keySELECT 	: {isKeyword(input, "SELECT")}? ID;
keyDELETE 	: {isKeyword(input, "DELETE")}? ID;
keyCREATE 	: {isKeyword(input, "CREATE")}? ID;
keyDESCRIBE 	: {isKeyword(input, "DESCRIBE")}? ID;
keySHOW 	: {isKeyword(input, "SHOW")}? ID;
keyENABLE 	: {isKeyword(input, "ENABLE")}? ID;
keyDISABLE 	: {isKeyword(input, "DISABLE")}? ID;
keyDROP 	: {isKeyword(input, "DROP")}? ID;
keyTABLE 	: {isKeyword(input, "TABLE")}? ID;
keySCHEMA 	: {isKeyword(input, "SCHEMA")}? ID;
keySCHEMAS 	: {isKeyword(input, "SCHEMAS")}? ID;
keyTABLES 	: {isKeyword(input, "TABLES")}? ID;
keyWHERE	: {isKeyword(input, "WHERE")}? ID;
keyWITH		: {isKeyword(input, "WITH")}? ID;
keyFILTER	: {isKeyword(input, "FILTER")}? ID;
keyFROM 	: {isKeyword(input, "FROM")}? ID;
keySET 		: {isKeyword(input, "SET")}? ID;
keyIN 		: {isKeyword(input, "IN")}? ID;
keyIS 		: {isKeyword(input, "IS")}? ID;
keyIF 		: {isKeyword(input, "IF")}? ID;
keyALIAS	: {isKeyword(input, "ALIAS")}? ID;
keyAS		: {isKeyword(input, "AS")}? ID;
keyTHEN 	: {isKeyword(input, "THEN")}? ID;
keyELSE 	: {isKeyword(input, "ELSE")}? ID;
keyEND 		: {isKeyword(input, "END")}? ID;
keyFIRST	: {isKeyword(input, "FIRST")}? ID;
keyLAST		: {isKeyword(input, "LAST")}? ID;
keyLIMIT 	: {isKeyword(input, "LIMIT")}? ID;
keyLIKE		: {isKeyword(input, "LIKE")}? ID;
keyTO 		: {isKeyword(input, "TO")}? ID;
keyOR 		: {isKeyword(input, "OR")}? ID;
keyAND 		: {isKeyword(input, "AND")}? ID;
keyNOT 		: {isKeyword(input, "NOT")}? ID;
keyTRUE 	: {isKeyword(input, "TRUE")}? ID;
keyFALSE 	: {isKeyword(input, "FALSE")}? ID;
keyBETWEEN 	: {isKeyword(input, "BETWEEN")}? ID;
keyNULL 	: {isKeyword(input, "NULL")}? ID;
keyFOR	 	: {isKeyword(input, "FOR")}? ID;
keyCLIENT	: {isKeyword(input, "CLIENT")}? ID;
keySERVER	: {isKeyword(input, "SERVER")}? ID;
keyVERSIONS	: {isKeyword(input, "VERSIONS")}? ID;
keyVERSION	: {isKeyword(input, "VERSION")}? ID;
keyTIMESTAMP	: {isKeyword(input, "TIMESTAMP")}? ID;
keyRANGE	: {isKeyword(input, "RANGE")}? ID;
keyMAX		: {isKeyword(input, "MAX")}? ID;
keyKEYS		: {isKeyword(input, "KEYS")}? ID;
keyALL		: {isKeyword(input, "ALL")}? ID;
keyCONTAINS	: {isKeyword(input, "CONTAINS")}? ID;
keyDEFAULT	: {isKeyword(input, "DEFAULT")}? ID;
keyCASE		: {isKeyword(input, "CASE")}? ID;
keyWHEN		: {isKeyword(input, "WHEN")}? ID;
keyINSERT	: {isKeyword(input, "INSERT")}? ID;
keyINTO		: {isKeyword(input, "INTO")}? ID;
keyVALUES	: {isKeyword(input, "VALUES")}? ID;
keyHELP		: {isKeyword(input, "HELP")}? ID;
keyPARSE	: {isKeyword(input, "PARSE")}? ID;
keyEXPR		: {isKeyword(input, "EXPR")}? ID;
keyIMPORT	: {isKeyword(input, "IMPORT")}? ID;
keyTEMP		: {isKeyword(input, "TEMP")}? ID;
keyBLOOM	: {isKeyword(input, "BLOOM")}? ID;
keyBLOCK	: {isKeyword(input, "BLOCK")}? ID;
keySIZE		: {isKeyword(input, "SIZE")}? ID;
keyCACHE	: {isKeyword(input, "CACHE")}? ID;
keyCOMPRESSION	: {isKeyword(input, "COMPRESSION")}? ID;
keyTYPE		: {isKeyword(input, "TYPE")}? ID;
keyMEMORY	: {isKeyword(input, "MEMORY")}? ID;
keyMAP		: {isKeyword(input, "MAP")}? ID;
keyFILE		: {isKeyword(input, "FILE")}? ID;
keyINDEX	: {isKeyword(input, "INDEX")}? ID;
keyINTERVAL	: {isKeyword(input, "INTERVAL")}? ID;
keyTTL		: {isKeyword(input, "TTL")}? ID;
