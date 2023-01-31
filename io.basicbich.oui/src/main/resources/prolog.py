import itertools
import json
import csv
import sys
from typing import Dict, Callable, List

Filter = Callable[[any], list]


def entries(item):
    if isinstance(item, dict):
        it = item.items()
    elif isinstance(item, list):
        it = enumerate(item)
    else:
        raise Exception("Cannot iterate over {}".format(type(item)))

    return [{"key": key, "value": value} for key, value in it]


def keys(key: str, value: str, item: any) -> list:
    if isinstance(item, dict):
        if item.get(key) == value:
            return [item]
        else:
            return []
    elif isinstance(item, list):
        return [i for i in item if i.get(key) == value]
    else:
        raise Exception("Cannot iterate over {}".format(type(item)))


# TODO: implement other filters

class SelectorFragment:
    def __init__(self):
        pass

    def __call__(self, item: any) -> List[any]:
        raise NotImplementedError()


class PropertySelectorFragment(SelectorFragment):
    def __init__(self, key: str):
        super().__init__()
        self.key = key

    def __call__(self, item: any) -> List[any]:
        return [item[self.key]]


class IndexSelectorFragment(SelectorFragment):
    def __init__(self, indexes: List[int]):
        super().__init__()
        self.indexes = indexes

    def __call__(self, item: any) -> List[any]:
        if len(self.indexes) == 0:
            return item
        return [item[index] for index in self.indexes]


class SliceSelectorFragment(SelectorFragment):
    def __init__(self, start: int, stop: int):
        super().__init__()
        self.start = start
        self.stop = stop

    def __call__(self, item: any) -> List[any]:
        return item[self.start:self.stop]


class Instruction:
    def __init__(self):
        pass

    def __call__(self, item: any, declarations: Dict[str, any]) -> List[any]:
        raise NotImplementedError()


class Selector(Instruction):
    def __init__(self, fragments: List[SelectorFragment], scope: str | Filter | None = None):
        super().__init__()
        self.fragments = fragments
        self.scope = scope

    def __call__(self, item: any, declarations: Dict[str, any]) -> List[any]:
        if isinstance(self.scope, str):  # declaration scope
            result = [declarations[self.scope]]
        elif callable(self.scope):  # filter scope
            result = self.scope(item)
        else:  # root scope
            result = [item]

        for fragment in self.fragments:
            result = list(itertools.chain(*[fragment(item) for item in result]))
        return result


class Object(Instruction):
    def __init__(self, properties: Dict[str, Instruction]):
        super().__init__()
        self.properties = properties

    def __call__(self, item: any, declarations: Dict[str, any]) -> List[any]:
        # Compute all possible combinations of property values.
        properties = {name: instruction(item, declarations) for name, instruction in self.properties.items()}
        combinations = itertools.product(*properties.values())

        results = []
        for combination in combinations:
            # Create one object for each combination.
            results.append({name: value for name, value in zip(properties.keys(), combination)})

        return results


class InstructionSet(Instruction):
    def __init__(self, declarations: Dict[str, Instruction], instruction: Instruction, next: "InstructionSet" = None):
        super().__init__()
        self.declarations = declarations
        self.instruction = instruction
        self.next = next

    def __call__(self, item: any, declarations: Dict[str, any] = {}) -> List[any]:
        # Compute all possible combinations of declarations
        combined_declarations = {name: [value] for name, value in declarations.items()}
        for name, instruction in self.declarations.items():
            combined_declarations[name] = instruction(item, declarations)
        combinations = itertools.product(*combined_declarations.values())

        results = []
        for combination in combinations:
            # Compute current declarations from combination
            current_declarations = {name: value for name, value in zip(combined_declarations.keys(), combination)}

            result = self.instruction(item, current_declarations)
            if self.next is None:
                results += result
            else:
                for result_item in result:
                    results += self.next(result_item, current_declarations)
        return results


data = json.load(sys.stdin)
