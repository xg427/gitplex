package com.gitplex.server.search.query.regex;

import java.util.ArrayList;
import java.util.List;

public class OrLiterals extends LogicalLiterals {

	public OrLiterals(List<Literals> elements) {
		super(elements);
	}
	
	public OrLiterals(Literals...elements) {
		super(elements);
	}
	
	@Override
	public List<List<LeafLiterals>> flattern(boolean outmost) {
		List<List<LeafLiterals>> rows = new ArrayList<>();
		for (Literals element: getElements())
			rows.addAll(element.flattern(outmost));
		LiteralUtils.trim(rows, outmost);
		return rows;
	}

}
