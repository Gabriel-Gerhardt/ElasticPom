class PaperParser:
    def __init__(self):
        pass
    def normalize_paper_id(self, paper_id):
        left, _, right = paper_id.strip().partition('.')
        if len(left) == 3:
            left = '0' + left
        return f'{left}.{right}'